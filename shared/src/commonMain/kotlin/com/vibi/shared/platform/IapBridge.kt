package com.vibi.shared.platform

/**
 * Swift 가 구현해 K/N 으로 주입하는 IAP bridge. iOS 만 사용 — Android 는 별도 `BillingClient`
 * 통합 시점에 같은 패턴으로 확장 가능하지만 v1 은 stub.
 *
 * StoreKit2 (`Product.purchase()` 등) 는 Swift-only API 라 Kotlin/Native cinterop 으로 직접
 * 호출할 수 없다. Apple Sign-In 과 동일한 callback bridge 방식으로 Swift 측이 결제 흐름을
 * 처리하고 결과만 Kotlin 으로 전달.
 *
 * suspend 함수를 Swift 가 구현하기 어려워 callback 으로 단순화. `IosIapClient` 가 wrapper 로
 * `suspendCancellableCoroutine` 변환 + 반환된 [IapCancellable] 로 코루틴 취소 시 Swift Task
 * 도 취소.
 *
 * outcome 문자열 단일 source — Swift 와 Kotlin 양쪽이 같은 값 사용. 변경 시 두 쪽 모두 갱신.
 * (`"success"` | `"cancelled"` | `"failed"`)
 *
 * Transaction lifecycle 은 BFF 영수증 검증과 동기 — Swift 가 `Product.purchase()` 결과의
 * Transaction 을 즉시 `finish` 하지 않고 [finishTransaction] 호출을 기다린다. BFF 가 영수증을
 * 검증·가산한 뒤에만 finish 가 되어, BFF 호출 실패 시 다음 앱 실행에서 `Transaction.updates`
 * (→ [setTransactionListener]) 가 같은 transaction 을 다시 emit, 재시도 가능 (4.5.2 reject 방지).
 */
interface IapBridge {
    /**
     * StoreKit2 `Product.purchase()` 트리거. callback 은 정확히 1회 호출.
     *
     * @return 코루틴이 취소되면 [IapCancellable.cancel] 로 Swift Task 까지 전파. 이미 결제 popup
     *   이 떠 있으면 Swift 측이 무시 (Apple 가이드).
     * @param outcome `"success"` | `"cancelled"` | `"failed"`
     * @param transactionId `Transaction.id` 문자열 — success 일 때만 non-null, BFF idempotency key.
     * @param receipt `Transaction.jsonRepresentation` 의 base64 — success 일 때만 non-null.
     * @param errorMessage failed 일 때 사유.
     */
    fun purchase(
        productId: String,
        callback: (
            outcome: String,
            transactionId: String?,
            receipt: String?,
            errorMessage: String?,
        ) -> Unit,
    ): IapCancellable

    /**
     * `AppStore.sync()` 호출. consumable (크레딧) 은 보통 복원 대상 아니지만 App Store
     * 가이드라인 권장사항 충족 + 향후 비소비성/구독 도입 시 즉시 동작하도록 노출.
     *
     * @param outcome `"success"` | `"cancelled"` | `"failed"`
     */
    fun restorePurchases(
        callback: (outcome: String, errorMessage: String?) -> Unit,
    ): IapCancellable

    /**
     * BFF 가 영수증을 검증·가산한 뒤 호출. Swift 측이 캐시 중인 [Transaction] 을 `finish` 해서
     * StoreKit 의 unfinished queue 에서 제거한다. 모르는 [transactionId] (이미 finish 됐거나
     * 다른 세션에서 emit 된 것) 는 silent no-op.
     */
    fun finishTransaction(transactionId: String)

    /**
     * `Transaction.updates` 장기 listener 등록. Swift 가 앱 lifecycle 동안 단일 Task 로 loop 돌며
     * Ask-to-Buy 승인 / Family Sharing / crash 직후 미완료 transaction 등을 emit. listener 는
     * Kotlin 측 reconciler 가 BFF 영수증 검증을 다시 시도 → 성공 시 [finishTransaction] 호출.
     *
     * 동일 객체에 두 번 호출되면 기존 Task 는 취소하고 새로 시작 — 보통 앱 시작 시 1회만 등록.
     *
     * @param productId Transaction.productID — Kotlin 측이 BFF 요청 body 에 그대로 전달.
     * @param receipt Transaction.jsonRepresentation 의 base64.
     */
    fun setTransactionListener(
        listener: (
            transactionId: String,
            receipt: String,
            productId: String,
        ) -> Unit,
    )
}

/**
 * 진행 중인 StoreKit Task 의 취소 핸들. `suspendCancellableCoroutine.invokeOnCancellation`
 * 에서 호출되어 sheet dismiss / 화면 이탈 시 Swift 측 Task 도 같이 끊는다.
 */
interface IapCancellable {
    fun cancel()
}
