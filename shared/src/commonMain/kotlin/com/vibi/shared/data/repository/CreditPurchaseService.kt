package com.vibi.shared.data.repository

import com.vibi.shared.data.local.AuthTokenStore
import com.vibi.shared.data.local.CreditStore
import com.vibi.shared.data.local.UserSession
import com.vibi.shared.data.remote.api.BffApi
import com.vibi.shared.data.remote.dto.CreditPurchaseRequest
import com.vibi.shared.domain.model.IapPlatform

/**
 * IAP 영수증을 BFF 로 보내 검증·가산하는 단일 진입점.
 *
 * 두 호출자가 존재:
 *   1. `UserMenuViewModel.purchaseCredits` — 사용자가 결제 sheet 에서 시스템 popup 통과 직후.
 *   2. `IapTransactionReconciler` — `Transaction.updates` 가 비동기로 emit 한 transaction
 *      (Ask-to-Buy 승인 / Family Sharing / 직전 실행에서 unfinished).
 *
 * 두 흐름 모두 동일한 BFF 호출 + `CreditStore.setBalance` + 실패 시 `refreshBalance` 패턴을
 * 갖기에 본 service 가 SSOT — 분기는 호출자가 "성공 시 무엇을 finish 할지" 만 결정한다.
 *
 * [requireAuth] 가 true 면 로그아웃 상태에서 BFF 401 폭주를 막기 위해 호출 자체를 skip.
 * reconciler 처럼 사용자 의도와 무관하게 백그라운드 호출되는 경로에서만 true.
 */
class CreditPurchaseService(
    private val bffApi: BffApi,
    private val tokenStore: AuthTokenStore,
    private val userSession: UserSession,
    private val creditStore: CreditStore,
) {
    suspend fun redeemReceipt(
        productId: String,
        platform: IapPlatform,
        receipt: String,
        transactionId: String,
        requireAuth: Boolean = false,
    ): Result<Unit> {
        if (requireAuth && tokenStore.getValidToken() == null) {
            return Result.failure(NotAuthenticatedException)
        }
        val req = CreditPurchaseRequest(
            productId = productId,
            platform = platform.wireName,
            receipt = receipt,
            transactionId = transactionId,
        )
        return runCatching { bffApi.purchaseCredits(req) }
            .onSuccess { resp -> creditStore.setBalance(userSession.current(), resp.balance) }
            .onFailure { refreshBalanceQuietly() }
            .map { Unit }
    }

    private suspend fun refreshBalanceQuietly() {
        runCatching { bffApi.getCreditBalance() }
            .onSuccess { resp -> creditStore.setBalance(userSession.current(), resp.balance) }
    }

    /** 로그아웃 상태에서 reconciler 가 BFF 를 호출하지 않도록 표시하는 sentinel. */
    object NotAuthenticatedException : RuntimeException("not_authenticated")
}
