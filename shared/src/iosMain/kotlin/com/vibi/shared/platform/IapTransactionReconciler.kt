package com.vibi.shared.platform

import com.vibi.shared.data.repository.CreditPurchaseService
import com.vibi.shared.domain.model.IapPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * `Transaction.updates` 로 도착하는 비동기 transaction 을 BFF 영수증 검증으로 마무리.
 *
 * 호출 경로:
 *   1. Swift `IapBridgeImpl` 의 Task 가 `Transaction.updates` for-await 루프를 돈다.
 *   2. transaction 발견 → [IapBridge.setTransactionListener] 로 등록된 람다 호출.
 *   3. 본 reconciler 가 [CreditPurchaseService.redeemReceipt] 로 BFF 영수증 전달.
 *   4. BFF 가산 성공 → [IapBridge.finishTransaction] 호출 → Swift Transaction.finish.
 *   5. BFF 호출 실패 → finish 안 함. 다음 앱 실행에서 같은 transaction 이 다시 emit 되어 재시도.
 *
 * 미인증 상태(로그아웃) 일 때는 service 가 [CreditPurchaseService.NotAuthenticatedException]
 * 으로 즉시 실패 → finish 보류. 사용자가 재로그인하면 다음 emit 에서 자동 재시도.
 *
 * App Store 가이드라인 4.5.2 ("결제 후 사용자에게 credit 누락") 와 직접 묶이는 흐름.
 */
class IapTransactionReconciler(
    private val bridge: IapBridge,
    private val creditPurchaseService: CreditPurchaseService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        bridge.setTransactionListener { transactionId, receipt, productId ->
            scope.launch { reconcile(transactionId, receipt, productId) }
        }
    }

    private suspend fun reconcile(transactionId: String, receipt: String, productId: String) {
        val result = creditPurchaseService.redeemReceipt(
            productId = productId,
            platform = IapPlatform.APPLE,
            receipt = receipt,
            transactionId = transactionId,
            requireAuth = true,
        )
        result
            .onSuccess {
                println("[IapReconcile] success tx=$transactionId")
                bridge.finishTransaction(transactionId)
            }
            .onFailure { e ->
                if (e === CreditPurchaseService.NotAuthenticatedException) {
                    println("[IapReconcile] skipped (no auth) tx=$transactionId")
                } else {
                    println("[IapReconcile] failed tx=$transactionId cause=${e.message}")
                }
            }
    }
}
