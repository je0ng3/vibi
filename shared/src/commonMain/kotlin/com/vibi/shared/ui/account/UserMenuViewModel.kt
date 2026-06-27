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
import com.vibi.shared.platform.RewardedAdController
import com.vibi.shared.platform.RewardedAdOutcome
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 홈화면 우상단 유저 메뉴를 위한 ViewModel.
 *
 * 잔액 동기화 (`refreshBalance`) 는 홈(InputScreen) 진입 시 + 메뉴가 열릴 때 호출 — 분리 차감·구매·
 * 다른 기기 변경을 프로필 sheet 를 열지 않아도 크레딧 배지에 반영. 회원탈퇴 후 user-scoped Room row 는
 * UserSession.userId 변경으로 자동 격리되므로 별도 wipe 하지 않는다 (signOut 동작과 동일).
 */
class UserMenuViewModel(
    private val authRepository: AuthRepository,
    tokenStore: AuthTokenStore,
    private val creditStore: CreditStore,
    private val userSession: UserSession,
    private val bffApi: BffApi,
    private val creditPurchaseService: CreditPurchaseService,
    private val rewardedAdController: RewardedAdController,
) : ViewModel() {

    data class UiState(
        val user: AuthUser?,
        val credits: Int,
        val adReward: AdRewardState = AdRewardState(),
    ) {
        val isAdmin: Boolean get() = user?.isAdmin == true
    }

    /**
     * 보상형 광고 현황 — Research preview 카드의 "광고 보고 1크레딧 받기" 버튼 상태.
     *
     * - [loaded]    — BFF status 1회 이상 조회 완료. false 면 UI 가 버튼 비활성(현황 미확인).
     * - [remaining] — 오늘 남은 시청 횟수 (서버 일일 상한 기준). 0 이면 비활성.
     * - [dailyCap]  — 하루 상한 ("오늘 N/cap" 표시용).
     * - [watching]  — 광고 표시 중 + 보상 반영(잔액 폴링) 진행 중. 중복 탭/스피너 처리.
     */
    data class AdRewardState(
        val loaded: Boolean = false,
        val remaining: Int = 0,
        val dailyCap: Int = 0,
        val watching: Boolean = false,
        /** 직전 시도가 광고 로드 실패(no-fill/오프라인 등)였는지 — UI 가 "광고 없음" 안내. 재시도 가능. */
        val noAdAvailable: Boolean = false,
    )

    private val _adReward = MutableStateFlow(AdRewardState())

    val uiState: StateFlow<UiState> = combine(
        tokenStore.cachedUser,
        creditStore.balance,
        _adReward,
    ) { user, credits, adReward -> UiState(user = user, credits = credits, adReward = adReward) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(
                user = tokenStore.cachedUser.value,
                credits = creditStore.balance.value,
                adReward = _adReward.value,
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

    /** 메뉴 진입 시 호출 — 오늘 광고 보상 남은 횟수/상한을 BFF 에서 가져와 버튼 상태에 반영. */
    fun refreshAdRewardStatus() {
        viewModelScope.launch { fetchAdRewardStatus() }
    }

    private suspend fun fetchAdRewardStatus() {
        runCatching { bffApi.getAdMobRewardStatus() }
            .onSuccess { r ->
                _adReward.update {
                    it.copy(loaded = true, remaining = r.remaining, dailyCap = r.dailyCap)
                }
            }
    }

    /**
     * "광고 보고 1크레딧 받기" — 보상형 광고를 표시한다. 끝까지 시청하면 Google 이 BFF SSV 콜백으로
     * +1 크레딧을 지급하므로(클라가 직접 지급 X), 시청 완료 후 잔액이 증가할 때까지 잠깐 폴링해
     * UI 에 반영한다. 마지막에 남은 횟수를 다시 갱신한다.
     */
    fun watchAdForCredit() {
        if (_adReward.value.watching) return
        viewModelScope.launch {
            _adReward.update { it.copy(watching = true, noAdAvailable = false) }
            var unavailable = false
            try {
                val before = creditStore.balance.value
                when (
                    runCatching { rewardedAdController.showRewardedAd(userSession.current()) }
                        .getOrElse { RewardedAdOutcome.UNAVAILABLE }
                ) {
                    RewardedAdOutcome.REWARD_EARNED -> pollBalanceUntilIncreased(before)
                    RewardedAdOutcome.UNAVAILABLE -> unavailable = true
                    RewardedAdOutcome.DISMISSED -> Unit // 보상 전 닫음 — 조용히 종료
                }
                fetchAdRewardStatus()
            } finally {
                _adReward.update { it.copy(watching = false, noAdAvailable = unavailable) }
            }
        }
    }

    /**
     * SSV 는 Google→BFF 서버간 비동기라 시청 직후 잔액이 아직 안 올랐을 수 있다. 최대 ~6초 동안
     * 1초 간격으로 폴링해 증가를 감지하면 종료. 끝내 못 감지해도 다음 refreshBalance 에서 반영된다.
     */
    private suspend fun pollBalanceUntilIncreased(before: Int) {
        val attempts = 6
        repeat(attempts) { attempt ->
            val resp = runCatching { bffApi.getCreditBalance() }.getOrNull()
            if (resp != null) {
                creditStore.setBalance(userSession.current(), resp.balance)
                if (resp.balance > before) return
            }
            if (attempt < attempts - 1) delay(1_000) // 마지막 시도 뒤에는 불필요한 대기 생략
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
                productId = "vibi.credits.starter",
                credits = 50,
                priceLabel = "$4.99",
                title = "Starter",
                subtitle = "50 credits",
            ),
            CreditProduct(
                productId = "vibi.credits.pro",
                credits = 200,
                priceLabel = "$14.99",
                title = "Pro",
                subtitle = "200 credits · 25% off",
                highlight = true,
            ),
            CreditProduct(
                productId = "vibi.credits.studio",
                credits = 1000,
                priceLabel = "$49.99",
                title = "Studio",
                subtitle = "1000 credits · 50% off",
            ),
        )
    }
}
