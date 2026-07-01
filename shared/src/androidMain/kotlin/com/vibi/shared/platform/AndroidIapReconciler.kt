package com.vibi.shared.platform

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.Purchase
import com.vibi.shared.data.repository.CreditPurchaseService
import com.vibi.shared.domain.model.IapPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * iOS [IapTransactionReconciler] 의 Android 동등물. Play Billing 은 `Transaction.updates` 같은
 * 장기 listener 가 없으므로 [AndroidIapClient.queryUnconsumedPurchases] 로 미완료 purchase 를
 * 발견 → BFF 영수증 검증 재시도 → 성공 시 consume 한다. iOS 의 `Transaction.updates` 가
 * "앱 실행 중 비동기로 계속 emit" 하는 것과 등가가 되도록 세 경로로 재조정한다:
 *
 *   1. **앱 시작 1회** — [start] 의 첫 [reconcileNow] 패스 (`VibiApplication.onCreate` 직후).
 *   2. **매 foreground** — [androidx.lifecycle.ProcessLifecycleOwner] `ON_START` 마다 재조정.
 *      iOS 가 다른 기기/Ask-to-Buy 승인분을 포그라운드 복귀 시 받는 흐름과 동등.
 *   3. **지연 완료 push** — [AndroidIapClient.deferredCompletions] (foreground purchase() 가
 *      직접 처리하지 못한 PENDING→PURCHASED 전이 등) 를 collect 해 즉시 redeem.
 *
 * 인증되지 않은 상태에서도 BFF 401 폭주는 [CreditPurchaseService.redeemReceipt]`(requireAuth = true)`
 * 가 [CreditPurchaseService.NotAuthenticatedException] 로 skip 처리.
 *
 * 미완료 케이스:
 *   - 직전 세션에서 BFF 가산 직전 앱 강제 종료 → 다음 부팅/포그라운드에 query 가 같은 token 재제출.
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
        // 1) 앱 시작 1회 패스 — lifecycle-process 미존재 시에도 기존 동작을 보장한다.
        scope.launch { reconcileNow() }
        // 3) 지연 완료(Ask-to-Buy 승인, PENDING→PURCHASED 전이 등) push 라우팅.
        iap.deferredCompletions
            .onEach { reconcile(it) }
            .launchIn(scope)
        // 2) 매 foreground 재조정 — lifecycle-process(optional dep) 가 있을 때만 활성.
        registerForegroundReconcile()
    }

    /**
     * 미완료 purchase 를 모두 query → redeem → 성공 시 consume 하는 단일 패스. iOS restore
     * (`AppStore.sync()` 후 entitlement 재검증) 와 동등 — "Restore Purchases" UI 및 foreground
     * 재조정이 공유한다. query 자체(연결 실패 등)만 [Result.failure] 로 표면화하고, 개별 redeem
     * 실패는 [reconcile] 내부에서 흡수돼 다음 패스에서 재시도된다.
     */
    suspend fun reconcileNow(): Result<Unit> = runCatching {
        iap.queryUnconsumedPurchases().forEach { reconcile(it) }
    }

    /**
     * [androidx.lifecycle.ProcessLifecycleOwner] 의 `ON_START` 마다 [reconcileNow] 를 돌린다.
     * `lifecycle-process` 는 optional dependency — 미추가 시 reflection 이 `ClassNotFoundException`
     * 으로 실패하고 본 경로만 조용히 비활성화된다 (시작 1회 패스 + restore + 지연완료 push 는
     * 유지). 의존성을 추가하면 컴파일 변경 없이 자동 활성화. observer 등록은 main thread 필수
     * (LifecycleRegistry 규약) 이므로 [Dispatchers.Main] 에서 수행.
     */
    private fun registerForegroundReconcile() {
        scope.launch(Dispatchers.Main) {
            runCatching {
                val processOwner = Class.forName("androidx.lifecycle.ProcessLifecycleOwner")
                    .getMethod("get")
                    .invoke(null) as LifecycleOwner
                processOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        scope.launch { reconcileNow() }
                    }
                })
            }
        }
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
