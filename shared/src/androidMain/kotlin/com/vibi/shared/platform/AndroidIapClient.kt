package com.vibi.shared.platform

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Google Play Billing Library 7.x wrapper — iOS 의 [IosIapClient] + Swift `IapBridgeImpl` 와
 * 동등한 역할.
 *
 * **결제 흐름** (foreground)
 *   1. [purchase] 호출 → [ensureConnected] → ProductDetails 조회 → [launchBillingFlow].
 *   2. Google Play UI 가 결제 popup 표시 → 사용자 confirm/cancel.
 *   3. [purchasesListener] 가 [purchaseUpdates] 로 emit → suspend 가 결과 await.
 *   4. Success 일 때 [txToToken] 에 `orderId → purchaseToken` 매핑 보관 — 호출자가 BFF 영수증
 *      검증 성공 후 [finishTransaction] 으로 consume 트리거.
 *
 * **lifecycle 규약**
 *   - Play Billing 정책: 결제 후 3일 내 [consumePurchase] (consumable) 또는 acknowledge 안 하면
 *     자동 환불. consume 은 ack 도 겸함 → BFF redeem 성공 후에만 consume 호출.
 *   - BFF redeem 실패 → consume 안 함 → 다음 부팅에 [queryUnconsumedPurchases] 가 동일 token
 *     재제출 ([AndroidIapReconciler] 가 활용). iOS [IapTransactionReconciler] 와 같은 패턴.
 *   - [BillingClient.ConnectionState] 가 disconnect 되면 다음 ensure 호출 시 자동 재연결.
 */
class AndroidIapClient(
    appContext: Context,
    private val activityProvider: ActivityProvider,
) {
    /**
     * [BillingClient.setListener] 의 콜백을 단일 flow 로 fan-out. foreground purchase 한 건만
     * 동시에 진행되므로 [first] 로 consume — replay/buffer 가 필요한 reconciler 경로는 별도
     * [queryUnconsumedPurchases] 로 처리한다 (Google 권장).
     */
    private val purchaseUpdates = MutableSharedFlow<PurchasesUpdated>(extraBufferCapacity = 16)

    /**
     * 진행 중 foreground [purchase] 호출 수. 0 일 때 도착한 OK + PURCHASED 업데이트는 사용자가
     * 직접 트리거하지 않은 "지연 완료" (Ask-to-Buy 승인 / PENDING→PURCHASED 전이 / 다른 세션)
     * 로 보고 [deferredPurchaseFlow] 로 흘려보낸다. iOS `Transaction.updates` 비동기 emit 동등.
     */
    private val activePurchases = AtomicInteger(0)

    private val deferredPurchaseFlow = MutableSharedFlow<Purchase>(extraBufferCapacity = 16)

    /**
     * 외부([AndroidIapReconciler]) 가 collect 하는 지연 완료 stream. foreground [purchase] 가
     * 직접 처리하지 못한 PURCHASED 만 emit — reconciler 가 BFF redeem → consume 로 마무리.
     */
    val deferredCompletions: SharedFlow<Purchase> get() = deferredPurchaseFlow

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        val list = purchases.orEmpty()
        purchaseUpdates.tryEmit(PurchasesUpdated(result, list))
        // foreground purchase() 가 결과를 await 중이 아니면(=지연 완료) PURCHASED 를 reconciler 로
        // 라우팅. 진행 중인 purchase() 가 있으면 그 호출이 직접 처리하므로 이중 redeem 방지.
        if (activePurchases.get() == 0 &&
            result.responseCode == BillingClient.BillingResponseCode.OK
        ) {
            list.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    deferredPurchaseFlow.tryEmit(purchase)
                }
            }
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .setListener(purchasesListener)
        .build()

    private val connectionMutex = Mutex()

    /** finishTransaction 등 fire-and-forget 호출 전용 scope. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // orderId → purchaseToken 매핑. iOS pendingTransactions dict 동등물.
    // foreground purchase 가 success 시 add, finishTransaction 이 remove + consume.
    private val txToToken = mutableMapOf<String, String>()
    private val txMutex = Mutex()

    private suspend fun ensureConnected() = connectionMutex.withLock {
        if (client.connectionState == BillingClient.ConnectionState.CONNECTED) return@withLock
        suspendCancellableCoroutine<Unit> { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        if (cont.isActive) cont.resume(Unit)
                    } else if (cont.isActive) {
                        cont.resumeWithException(
                            IllegalStateException("billing_setup_failed: ${result.debugMessage}")
                        )
                    }
                }
                override fun onBillingServiceDisconnected() {
                    // 재연결은 다음 ensureConnected 호출 시 startConnection 재호출로.
                }
            })
        }
    }

    suspend fun purchase(productId: String): AndroidPurchaseOutcome {
        runCatching { ensureConnected() }.onFailure {
            return AndroidPurchaseOutcome.Failed(it.message ?: "billing_setup_failed")
        }
        val activity = activityProvider.current
            ?: return AndroidPurchaseOutcome.Failed("no_activity")
        val product = runCatching { queryProduct(productId) }.getOrNull()
            ?: return AndroidPurchaseOutcome.Failed("product_not_found")

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .build()
                )
            )
            .build()

        // launch ~ 결과 await 구간 동안 purchasesListener 의 지연-완료 라우팅을 억제 —
        // 본 호출이 결과를 직접 처리하므로 reconciler 와 이중 redeem 되지 않게 한다.
        activePurchases.incrementAndGet()
        try {
            val launchResult = client.launchBillingFlow(activity, flowParams)
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                return AndroidPurchaseOutcome.Failed("launch_failed: ${launchResult.debugMessage}")
            }
            val update = purchaseUpdates.first { it.matches(productId) }
            return mapUpdate(productId, update)
        } finally {
            activePurchases.decrementAndGet()
        }
    }

    /**
     * [BillingClient.queryPurchasesAsync] 결과 — purchaseState = PURCHASED 인 INAPP 항목만.
     * Google 권장: 앱 시작 / foreground 진입 시 1회 호출해 미완료 (BFF 가산 실패 / 다른 세션
     * 결제 / Play Store 보류) 인 purchase 를 발견 → [AndroidIapReconciler] 가 BFF redeem 재시도.
     */
    suspend fun queryUnconsumedPurchases(): List<Purchase> {
        ensureConnected()
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = client.queryPurchasesAsync(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return emptyList()
        }
        return result.purchasesList.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
    }

    /**
     * BFF 가산 성공 후 호출. [transactionId] 는 [AndroidPurchaseOutcome.Success.transactionId]
     * 또는 [Purchase.getOrderId]. consume 은 idempotent — 이미 consume 된 token 은 silent OK.
     *
     * iOS [IapBridge.finishTransaction] 와 같은 fire-and-forget 시그니처 (non-suspend).
     * 호출자는 결과 대기 불필요 — consume 실패는 다음 부팅에서 reconciler 가 자동 재시도.
     */
    fun finishTransaction(transactionId: String) {
        scope.launch {
            val token = txMutex.withLock { txToToken.remove(transactionId) }
            if (token != null) {
                consumeQuietly(token)
                return@launch
            }
            // 캐시 miss — reconciler 가 처리한 케이스. orderId → token 매핑이 없으므로 직접 query 후
            // 매칭. Play Billing 은 orderId 가 [Purchase.getOrderId] 로 노출.
            runCatching { queryUnconsumedPurchases() }
                .getOrNull()
                ?.firstOrNull { it.orderId == transactionId }
                ?.let { consumeQuietly(it.purchaseToken) }
        }
    }

    suspend fun consumePurchase(purchaseToken: String) {
        ensureConnected()
        val params = com.android.billingclient.api.ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        client.consumePurchase(params)
    }

    private suspend fun queryProduct(productId: String): ProductDetails? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return null
        return result.productDetailsList?.firstOrNull()
    }

    private suspend fun mapUpdate(productId: String, update: PurchasesUpdated): AndroidPurchaseOutcome {
        return when (update.result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = update.purchases.firstOrNull { it.products.contains(productId) }
                    ?: return AndroidPurchaseOutcome.Failed("purchase_missing")
                if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
                    return AndroidPurchaseOutcome.Failed("purchase_pending")
                }
                val txId = purchase.orderId ?: purchase.purchaseToken
                txMutex.withLock { txToToken[txId] = purchase.purchaseToken }
                AndroidPurchaseOutcome.Success(
                    productId = productId,
                    transactionId = txId,
                    receipt = purchase.purchaseToken,
                )
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> AndroidPurchaseOutcome.Cancelled
            else -> AndroidPurchaseOutcome.Failed(update.result.debugMessage.ifEmpty { "billing_error_${update.result.responseCode}" })
        }
    }

    private suspend fun consumeQuietly(purchaseToken: String) {
        runCatching { consumePurchase(purchaseToken) }
    }

    private data class PurchasesUpdated(
        val result: BillingResult,
        val purchases: List<Purchase>,
    ) {
        fun matches(productId: String): Boolean {
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return true
            return purchases.any { it.products.contains(productId) }
        }
    }
}

sealed interface AndroidPurchaseOutcome {
    data class Success(
        val productId: String,
        val transactionId: String,
        val receipt: String,
    ) : AndroidPurchaseOutcome
    data object Cancelled : AndroidPurchaseOutcome
    data class Failed(val message: String) : AndroidPurchaseOutcome
}
