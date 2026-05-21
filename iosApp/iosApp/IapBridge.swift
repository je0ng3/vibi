import Cmp
import Foundation
import StoreKit

/// Swift implementation of the Kotlin `IapBridge` interface (defined in `:shared` commonMain).
/// 호출자(`IosIapClient`)가 callback 을 suspend 함수로 변환한다.
///
/// StoreKit2 (iOS 15+) — `Product.purchase()` 의 시스템 결제 popup 은 Apple 이 그린다.
/// 본 클래스는 trigger + 결과 매핑만; UI 모방 없음 (App Store 가이드라인 3.1.1 / 4.1 / 4.5).
///
/// 세션 동안 `Product` 객체는 캐시 — 매 구매마다 카탈로그 RTT (~100~500ms) 방지.
///
/// **Transaction lifecycle 규약**:
///   - `Product.purchase()` 결과 transaction 은 즉시 `finish()` 하지 않고
///     `pendingTransactions[txId] = tx` 로 보관 → `finishTransaction(txId)` 호출 시점에 finish.
///     호출자(Kotlin) 는 BFF 가 영수증을 검증·가산한 직후에만 finish 한다.
///   - `Transaction.updates` for-await 루프가 Ask-to-Buy 승인 / Family Sharing / 직전 실행에서
///     finish 못한 transaction 등을 emit → `transactionListener` 람다로 Kotlin 측에 전달.
///     Kotlin reconciler 가 BFF 영수증 검증 후 `finishTransaction` 으로 마무리.
///   - finish 하지 않은 transaction 은 다음 앱 실행에서 또 `Transaction.updates` 가 emit —
///     사용자가 결제 후 credit 누락되는 케이스 (4.5.2 reject 사유) 차단.
final class IapBridgeImpl: NSObject, IapBridge {

    // outcome 문자열은 Kotlin `IapBridge` 의 expected 값과 동기. 변경 시 양쪽 모두 갱신.
    private enum Outcome {
        static let success = "success"
        static let cancelled = "cancelled"
        static let failed = "failed"
    }

    private var productCache: [String: Product] = [:]
    private let cacheQueue = DispatchQueue(label: "vibi.iap.cache")

    private var pendingTransactions: [String: Transaction] = [:]
    private let pendingQueue = DispatchQueue(label: "vibi.iap.pending")

    private var updatesTask: Task<Void, Never>?

    func purchase(
        productId: String,
        callback: @escaping (String, String?, String?, String?) -> Void
    ) -> IapCancellable {
        let task = Task {
            do {
                let product = try await cachedProduct(productId: productId)
                guard let product = product else {
                    callback(Outcome.failed, nil, nil, "product_not_found")
                    return
                }
                try Task.checkCancellation()
                let result = try await product.purchase()
                try Task.checkCancellation()

                switch result {
                case .success(let verification):
                    switch verification {
                    case .verified(let tx):
                        // Transaction.jsonRepresentation 은 Apple 서명된 JWS — BFF 가 App Store
                        // Server API 로 재검증하므로 그대로 base64 인코딩 전달.
                        let receiptB64 = tx.jsonRepresentation.base64EncodedString()
                        let txKey = "\(tx.id)"
                        // foreground 가 BFF 호출 후 finishTransaction 부를 때까지 보관. 동시에
                        // Transaction.updates listener 가 같은 tx 를 emit 해도 이 dict 가
                        // "foreground 가 이미 처리 중" 마커로 작동해 중복 BFF 호출 차단.
                        pendingQueue.sync { pendingTransactions[txKey] = tx }
                        callback(Outcome.success, txKey, receiptB64, nil)
                    case .unverified(_, let error):
                        callback(Outcome.failed, nil, nil, "verification_failed: \(error.localizedDescription)")
                    }
                case .userCancelled:
                    callback(Outcome.cancelled, nil, nil, nil)
                case .pending:
                    // Ask-to-Buy / SCA 보류. 부모 승인 후 Transaction.updates 로 도착 —
                    // transactionListener 가 BFF 가산을 처리.
                    callback(Outcome.failed, nil, nil, "purchase_pending")
                @unknown default:
                    callback(Outcome.failed, nil, nil, "unknown_result")
                }
            } catch is CancellationError {
                return
            } catch {
                callback(Outcome.failed, nil, nil, error.localizedDescription)
            }
        }
        return CancellableTask(task: task)
    }

    func restorePurchases(callback: @escaping (String, String?) -> Void) -> IapCancellable {
        let task = Task {
            do {
                // consumable 은 보통 복원 대상 아님. 권장사항 충족 + 향후 비소비성/구독 도입 시
                // 즉시 동작하도록 AppStore.sync() 호출.
                try await AppStore.sync()
                try Task.checkCancellation()
                callback(Outcome.success, nil)
            } catch is CancellationError {
                return
            } catch {
                callback(Outcome.failed, error.localizedDescription)
            }
        }
        return CancellableTask(task: task)
    }

    func finishTransaction(transactionId: String) {
        let cached = pendingQueue.sync { pendingTransactions.removeValue(forKey: transactionId) }
        if let cached = cached {
            Task { await cached.finish() }
            return
        }
        // 캐시 miss — 다른 세션이 emit 한 transaction 일 수 있어 Transaction.unfinished 를 한 번
        // 훑어 동일 id 찾기. unfinished 는 finite snapshot AsyncSequence 라 자연 종료.
        Task {
            for await result in Transaction.unfinished {
                if case .verified(let tx) = result, "\(tx.id)" == transactionId {
                    await tx.finish()
                    return
                }
            }
        }
    }

    func setTransactionListener(
        listener: @escaping (String, String, String) -> Void
    ) {
        updatesTask?.cancel()
        updatesTask = Task.detached { [weak self] in
            for await result in Transaction.updates {
                guard let self = self else { return }
                guard case .verified(let tx) = result else {
                    // .unverified — Apple 서명 검증 실패. 위변조 의심 transaction 은 finish 도
                    // 안 함 (Apple 권장).
                    continue
                }
                let txKey = "\(tx.id)"
                // foreground purchase() 가 이미 캐시한 tx — Kotlin 측이 처리 중이므로 listener
                // 호출 skip (BFF idempotent 지만 중복 RTT 회피).
                let alreadyPending = self.pendingQueue.sync {
                    self.pendingTransactions[txKey] != nil
                }
                if alreadyPending { continue }
                let receipt = tx.jsonRepresentation.base64EncodedString()
                self.pendingQueue.sync { self.pendingTransactions[txKey] = tx }
                listener(txKey, receipt, tx.productID)
            }
        }
    }

    private func cachedProduct(productId: String) async throws -> Product? {
        if let cached = cacheQueue.sync(execute: { productCache[productId] }) {
            return cached
        }
        let products = try await Product.products(for: [productId])
        guard let product = products.first else { return nil }
        cacheQueue.sync { productCache[productId] = product }
        return product
    }
}

/// Kotlin `IapCancellable` 의 Swift 구현. `IosIapClient.suspendCancellableCoroutine.invokeOnCancellation`
/// 에서 호출되어 Swift Task 까지 취소 — 결제 popup 중 sheet dismiss 시 receipt 누수 방지.
private final class CancellableTask: IapCancellable {
    private let task: Task<Void, Never>
    init(task: Task<Void, Never>) { self.task = task }
    func cancel() { task.cancel() }
}
