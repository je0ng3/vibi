package com.vibi.cmp.platform

import com.vibi.shared.domain.model.IapPlatform
import com.vibi.shared.platform.AndroidIapClient
import com.vibi.shared.platform.AndroidIapReconciler
import com.vibi.shared.platform.AndroidPurchaseOutcome
import org.koin.mp.KoinPlatform

/**
 * Android actual — Google Play Billing 7.x ([AndroidIapClient]) 에 위임. iOS [IosIapClient]
 * 와 동일한 패턴 (Koin lazy + outcome 매핑).
 *
 * 결제 popup 은 Google Play 시스템이 그린다 (Play 정책). 본 클래스는 trigger + 결과 매핑만.
 *
 * [restorePurchases] 는 [AndroidIapReconciler.reconcileNow] 에 위임 — 미완료 purchase 를
 * query → BFF redeem → consume 까지 끝낸 뒤 결과를 반환한다 (Restore 명시 UI 도 startup /
 * foreground 재조정과 동일 경로를 재사용).
 */
actual class PurchaseLauncher actual constructor() {
    private val client: AndroidIapClient by lazy { KoinPlatform.getKoin().get() }
    private val reconciler: AndroidIapReconciler by lazy { KoinPlatform.getKoin().get() }

    actual suspend fun purchase(productId: String): PurchaseResult =
        when (val outcome = client.purchase(productId)) {
            is AndroidPurchaseOutcome.Success -> PurchaseResult.Success(
                productId = outcome.productId,
                transactionId = outcome.transactionId,
                receipt = outcome.receipt,
                platform = IapPlatform.GOOGLE,
            )
            AndroidPurchaseOutcome.Cancelled -> PurchaseResult.UserCancelled
            is AndroidPurchaseOutcome.Failed -> PurchaseResult.Failed(outcome.message)
        }

    actual suspend fun restorePurchases(): RestoreResult =
        reconciler.reconcileNow().fold(
            onSuccess = { RestoreResult.Completed },
            onFailure = { RestoreResult.Failed(it.message ?: "restore_failed") },
        )

    actual fun finishTransaction(transactionId: String) {
        client.finishTransaction(transactionId)
    }
}
