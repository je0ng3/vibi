package com.vibi.shared.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibi.shared.data.local.AuthTokenStore
import com.vibi.shared.data.local.CreditStore
import com.vibi.shared.data.local.UserSession
import com.vibi.shared.data.remote.api.BffApi
import com.vibi.shared.data.remote.dto.AdminGrantRequest
import com.vibi.shared.data.repository.AuthRepository
import com.vibi.shared.data.repository.CreditPurchaseService
import com.vibi.shared.domain.model.AuthUser
import com.vibi.shared.domain.model.IapPlatform
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

/**
 * 홈화면 우상단 유저 메뉴를 위한 ViewModel.
 *
 * 잔액 동기화 (`refreshBalance`) 는 메뉴가 열릴 때만 호출 — InputScreen 재진입 마다
 * BFF round-trip 을 발생시키지 않기 위함. 회원탈퇴 후 user-scoped Room row 는
 * UserSession.userId 변경으로 자동 격리되므로 별도 wipe 하지 않는다 (signOut 동작과 동일).
 */
class UserMenuViewModel(
    private val authRepository: AuthRepository,
    tokenStore: AuthTokenStore,
    private val creditStore: CreditStore,
    private val userSession: UserSession,
    private val bffApi: BffApi,
    private val creditPurchaseService: CreditPurchaseService,
) : ViewModel() {

    data class UiState(
        val user: AuthUser?,
        val credits: Int,
    ) {
        val isAdmin: Boolean get() = user?.isAdmin == true
    }

    val uiState: StateFlow<UiState> = combine(
        tokenStore.cachedUser,
        creditStore.balance,
    ) { user, credits -> UiState(user = user, credits = credits) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(
                user = tokenStore.cachedUser.value,
                credits = creditStore.balance.value,
            )
        )

    val products: List<CreditProduct> = CreditProduct.DEFAULTS

    private val _navigateToLogin = MutableSharedFlow<Unit>()
    val navigateToLogin: SharedFlow<Unit> = _navigateToLogin.asSharedFlow()

    fun refreshBalance() {
        viewModelScope.launch {
            runCatching { bffApi.getCreditBalance() }
                .onSuccess { resp -> creditStore.setBalance(userSession.current(), resp.balance) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { authRepository.signOut() }
            _navigateToLogin.emit(Unit)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            runCatching { authRepository.deleteAccount() }
            _navigateToLogin.emit(Unit)
        }
    }

    /**
     * IAP 시스템이 결제 성공 콜백을 돌려준 직후 호출. 호출자가 Result.isSuccess 일 때 transaction
     * finish 를 트리거 — 실패 시 finish 보류로 다음 앱 실행에서 Transaction.updates listener 가
     * 같은 영수증을 재제출 (4.5.2 reject 방지).
     */
    suspend fun purchaseCredits(
        product: CreditProduct,
        platform: IapPlatform,
        receipt: String,
        transactionId: String,
    ): Result<Unit> = creditPurchaseService.redeemReceipt(
        productId = product.productId,
        platform = platform,
        receipt = receipt,
        transactionId = transactionId,
    )

    /**
     * 관리자 무료 충전. 매 호출마다 BFF 가 새 txId (admin-<UUID>) 를 생성하므로 같은 상품을
     * 반복 탭하면 매번 가산되는 동작이 의도. BFF 가 `requireAdmin` 으로 role 검사 — 모바일이
     * 우회 시도해도 403.
     */
    suspend fun adminGrantCredits(product: CreditProduct): Result<Unit> {
        check(uiState.value.isAdmin) { "adminGrantCredits called by non-admin user" }
        val req = AdminGrantRequest(productId = product.productId)
        return runCatching { bffApi.adminGrantCredits(req) }
            .onSuccess { resp -> creditStore.setBalance(userSession.current(), resp.balance) }
            .onFailure { refreshBalance() }
            .map { Unit }
    }
}

/**
 * 크레딧 상품 한 건. 실제 SKU 와 매핑되는 [productId] + UI 표시용 [credits]/[priceLabel].
 *
 * **단위 규약**: 1 크레딧 = 영상 1분 구간의 음원 분리 / 배경음 분리. App Store Connect 의
 * product description 과 본 객체의 [subtitle] 모두 같은 단위를 사용해야 사용자·심사관 혼선
 * 없음. 단위 변경 시 ASC localized description 도 같이 수정.
 */
data class CreditProduct(
    val productId: String,
    val credits: Int,
    val priceLabel: String,
    val title: String,
    val subtitle: String? = null,
    val highlight: Boolean = false,
) {
    companion object {
        val DEFAULTS: List<CreditProduct> = listOf(
            CreditProduct(
                productId = "vibi.credits.10",
                credits = 10,
                priceLabel = "₩1,500",
                title = "10 credits",
                subtitle = "10 minutes · quick top-up",
            ),
            CreditProduct(
                productId = "vibi.credits.50",
                credits = 50,
                priceLabel = "₩6,900",
                title = "50 credits",
                subtitle = "50 minutes · 8% off",
                highlight = true,
            ),
            CreditProduct(
                productId = "vibi.credits.150",
                credits = 150,
                priceLabel = "₩18,000",
                title = "150 credits",
                subtitle = "150 minutes · 20% off",
            ),
        )
    }
}
