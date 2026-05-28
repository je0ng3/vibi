package com.vibi.shared.platform

import com.android.billingclient.api.Purchase
import com.vibi.shared.data.repository.CreditPurchaseService
import com.vibi.shared.domain.model.IapPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * iOS [IapTransactionReconciler] 의 Android 동등물. Play Billing 은 `Transaction.updates` 같은
 * 장기 listener 가 없으므로 **앱 시작 시 [AndroidIapClient.queryUnconsumedPurchases] 1회** 호출로
 * 미완료 purchase 를 모두 발견 → BFF 영수증 검증 재시도 → 성공 시 consume.
 *
 * 호출 시점:
 *   - `VibiApplication.onCreate` 직후 [start] 1회. 인증되지 않은 상태에서도 BFF 401 폭주는
 *     [CreditPurchaseService.redeemReceipt]`(requireAuth = true)` 가 [CreditPurchaseService.NotAuthenticatedException]
 *     로 skip 처리.
 *
 * 미완료 케이스:
 *   - 직전 세션에서 BFF 가산 직전 앱 강제 종료 → 다음 부팅에 query 가 같은 token 재제출.
 *   - Ask-to-Buy (가족) 보류 후 부모 승인 — 보통 [Purchase.PurchaseState.PENDING] → 승인 후
 *     PURCHASED 로 다시 emit. 본 reconciler 는 PURCHASED 만 처리.
 *   - 다른 기기에서 같은 Google 계정으로 구매한 consumable — 통상 자동 복원은 안 되지만
 *     Play 가 query 에 포함시키면 안전하게 가산.
 */
class AndroidIapReconciler(
    private val iap: AndroidIapClient,
    private val creditPurchaseService: CreditPurchaseService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch { reconcileAll() }
    }

    private suspend fun reconcileAll() {
        val purchases = runCatching { iap.queryUnconsumedPurchases() }.getOrNull() ?: return
        purchases.forEach { reconcile(it) }
    }

    private suspend fun reconcile(purchase: Purchase) {
        val productId = purchase.products.firstOrNull() ?: return
        val txId = purchase.orderId ?: purchase.purchaseToken
        val result = creditPurchaseService.redeemReceipt(
            productId = productId,
            platform = IapPlatform.GOOGLE,
            receipt = purchase.purchaseToken,
            transactionId = txId,
            requireAuth = true,
        )
        result
            .onSuccess {
                println("[IapReconcile/A] success tx=$txId")
                runCatching { iap.consumePurchase(purchase.purchaseToken) }
            }
            .onFailure { e ->
                if (e === CreditPurchaseService.NotAuthenticatedException) {
                    println("[IapReconcile/A] skipped (no auth) tx=$txId")
                } else {
                    println("[IapReconcile/A] failed tx=$txId cause=${e.message}")
                }
            }
    }
}
