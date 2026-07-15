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
import com.vibi.shared.domain.model.IdentityProvider
import com.vibi.shared.domain.model.LinkedIdentity
import com.vibi.shared.platform.RewardedAdController
import com.vibi.shared.platform.RewardedAdOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
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
        /** 직전 시도 결과 — UI 가 성공/지연/광고없음 안내를 띄우는 데 사용. 다음 시도·메뉴 재진입 시 [AdRewardResult.None] 으로 리셋. */
        val result: AdRewardResult = AdRewardResult.None,
    )

    /**
     * "광고 보고 1크레딧" 직전 시도의 결과. 보상은 서버(SSV)가 지급하므로 클라는 관측만 한다.
     * - [Granted] — 시청 완료 후 폴링 중 잔액 증가를 확인(지급 반영됨).
     * - [Pending] — 시청은 완료했으나 폴링 창(~6초) 안에 증가를 못 봄. SSV 가 Google→BFF 비동기라
     *               곧 반영될 수 있음. **무보상 침묵 방지**: 사용자에게 "곧 반영" 안내를 띄운다.
     * - [NoAd]    — 광고 로드 실패(no-fill/오프라인). 재시도 가능.
     * - [None]    — 표시할 결과 없음(초기 / 보상 전 닫음 / 메뉴 재진입 후).
     */
    enum class AdRewardResult { None, Granted, Pending, NoAd }

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

    /**
     * '계정 연결' 서브시트 상태.
     *
     * - [identities] — 연결된 로그인 수단. null = 아직 미로딩, emptyList 는 없음(이론상 없음).
     * - [loadFailed] — 목록 최초 조회 실패(오프라인·BFF 오류). UI 가 무한 스피너 대신 오류+재시도를 표시.
     * - [busyProvider] — 링크/언링크 진행 중인 provider(해당 버튼 스피너 + 중복 탭 방지). 없으면 null.
     * - [message]    — 직전 액션의 일회성 안내(성공/에러). 표시 후 [clearLinkMessage] 또는 다음 액션에서 리셋.
     */
    data class LinkUiState(
        val identities: List<LinkedIdentity>? = null,
        val loadFailed: Boolean = false,
        val busyProvider: IdentityProvider? = null,
        val message: LinkMessage? = null,
    )

    /** 링크/언링크 결과 안내 — 문구는 UI 가 매핑(i18n·톤 일관). [Merged] 만 이월 크레딧 수를 담는다. */
    sealed interface LinkMessage {
        data class Merged(val credits: Int) : LinkMessage
        data object Linked : LinkMessage
        data object AlreadyLinked : LinkMessage
        data object Unlinked : LinkMessage
        data object ProviderConflict : LinkMessage
        data object CannotUnlinkLast : LinkMessage
        data object NotLinked : LinkMessage
        data object TimedOut : LinkMessage
        data object Error : LinkMessage
    }

    private val _link = MutableStateFlow(LinkUiState())
    val link: StateFlow<LinkUiState> = _link.asStateFlow()

    /**
     * 세션(로그인 계정)에 종속된 in-flight 작업 — 목록 조회·링크·광고 폴링·잔액 갱신 등. 계정이 바뀌면
     * [resetSessionScopedState] 가 이 job 을 통째로 취소하고 완료를 기다린 뒤 상태를 리셋한다. 이렇게
     * 별도 job 으로 묶어야, 이전 계정의 늦게 도착한 응답(또는 finally)이 리셋 이후에 새 세션 상태를
     * 덮어써 이전 계정 데이터를 다시 노출하는 레이스를 막을 수 있다. [viewModelScope] 자식이라 VM
     * 소멸 시 함께 취소된다. signOut/deleteAccount 와 아래 collector 는 계정 전환에도 살아남아야
     * 하므로 이 scope 가 아니라 [viewModelScope] 에서 돈다.
     */
    private var sessionJob = SupervisorJob(viewModelScope.coroutineContext.job)
    private val sessionScope get() = CoroutineScope(viewModelScope.coroutineContext + sessionJob)

    init {
        // 이 VM 은 앱 전역 ViewModelStore 에 살아남는다 — 단순 stack 네비게이션(VibiNavHost)이라 화면별
        // store 가 없어, 로그아웃/세션만료(401) 후 다른 계정으로 로그인해도 같은 인스턴스가 재사용된다.
        // 그러면 이전 계정의 연결 목록·광고 캐시가 남고, [loadIdentities] 는 identities != null 이면
        // 조기 반환하므로 새 계정 화면에 이전 계정의 연결 목록이 그대로 보인다. 로그인 계정(JWT sub)이
        // 바뀌는 순간을 단일 지점에서 감지해 세션 종속 인메모리 상태를 폐기한다 — signOut(cachedUser→null)
        // ·회원탈퇴·401(계정 전환으로 sub 변경) 세 경로를 모두 덮는다.
        viewModelScope.launch {
            tokenStore.cachedUser
                .map { it?.sub }
                .distinctUntilChanged()
                .drop(1) // 최초 방출(현재 로그인)은 이미 기본 상태라 리셋 불필요
                .collect { resetSessionScopedState() }
        }
    }

    /**
     * 계정 전환 시 세션 종속 상태를 폐기한다. **순서가 핵심**: 이전 계정의 in-flight 작업을 취소하고
     * 그 finally/continuation 이 끝날 때까지(`cancelAndJoin`) 기다린 뒤에 상태를 리셋한다. 그래야
     * 늦게 완료된 이전 계정의 [watchAdForCredit] finally 나 [loadIdentities]/[finishLinkAction] 의
     * `_link` 쓰기가 리셋을 덮어써 이전 계정 데이터를 다시 노출하는 레이스가 없다. join 후 새 job 을
     * 만들어 이후 세션 작업이 정상 동작하게 한다.
     */
    private suspend fun resetSessionScopedState() {
        sessionJob.cancelAndJoin()
        sessionJob = SupervisorJob(viewModelScope.coroutineContext.job)
        _link.value = LinkUiState()
        _adReward.value = AdRewardState()
    }

    /**
     * '계정 연결' 시트 진입 시 호출 — 목록이 아직 없을 때만 BFF 조회한다. 링크/언링크는 서버가
     * 돌려준 목록을 [_link] 에 그대로 반영하므로, 시트를 다시 열 때 이미 최신 목록을 갖고 있어
     * 재조회는 낭비다(실패로 목록이 여전히 null 이면 다음 오픈에서 재시도된다).
     */
    fun loadIdentities() {
        if (_link.value.identities != null) return
        sessionScope.launch {
            _link.update { it.copy(loadFailed = false) }
            authRepository.listIdentities()
                .onSuccess { list -> _link.update { it.copy(identities = list) } }
                // 실패 시 identities 는 null 로 남는다 — UI 가 [loadFailed] 로 오류+재시도를 띄운다(무한 스피너 방지).
                .onFailure { _link.update { it.copy(loadFailed = true) } }
        }
    }

    fun linkGoogle() = runLink(IdentityProvider.GOOGLE) { authRepository.linkGoogle() }

    fun linkApple() = runLink(IdentityProvider.APPLE) { authRepository.linkApple() }

    private fun runLink(
        provider: IdentityProvider,
        block: suspend () -> AuthRepository.LinkResult,
    ) {
        if (_link.value.busyProvider != null) return
        // busy 마킹을 launch 밖(메인 스레드 동기)에서 먼저 — check-then-act 를 원자화해 빠른 더블탭이
        // 둘 다 가드를 통과(→ 중복 네이티브 sign-in / POST)하는 레이스를 막는다.
        _link.update { it.copy(busyProvider = provider, message = null) }
        sessionScope.launch {
            val result = block()
            // merged 는 계정 잔액이 바뀌므로 로컬 크레딧 캐시를 갱신 — 프로필의 크레딧 배지에 즉시 반영.
            if (result is AuthRepository.LinkResult.Success && result.newBalance != null) {
                creditStore.setBalance(userSession.current(), result.newBalance)
            }
            val message = when (result) {
                is AuthRepository.LinkResult.Success -> when (result.status) {
                    AuthRepository.LinkStatus.MERGED -> LinkMessage.Merged(result.mergedCredits ?: 0)
                    AuthRepository.LinkStatus.ALREADY_LINKED -> LinkMessage.AlreadyLinked
                    AuthRepository.LinkStatus.LINKED -> LinkMessage.Linked
                }
                AuthRepository.LinkResult.ProviderConflict -> LinkMessage.ProviderConflict
                AuthRepository.LinkResult.TimedOut -> LinkMessage.TimedOut
                is AuthRepository.LinkResult.Failed -> LinkMessage.Error
            }
            finishLinkAction((result as? AuthRepository.LinkResult.Success)?.identities, message)
        }
    }

    fun unlink(provider: IdentityProvider) {
        if (_link.value.busyProvider != null) return
        _link.update { it.copy(busyProvider = provider, message = null) } // 레이스 방지 — [runLink] 주석 참조.
        sessionScope.launch {
            val result = authRepository.unlinkIdentity(provider)
            val message = when (result) {
                is AuthRepository.UnlinkResult.Success -> LinkMessage.Unlinked
                AuthRepository.UnlinkResult.CannotUnlinkLast -> LinkMessage.CannotUnlinkLast
                AuthRepository.UnlinkResult.NotLinked -> LinkMessage.NotLinked
                is AuthRepository.UnlinkResult.Failed -> LinkMessage.Error
            }
            finishLinkAction((result as? AuthRepository.UnlinkResult.Success)?.identities, message)
        }
    }

    /** 링크/언링크 종료 공통 처리 — busy 해제 + (성공 시) 새 목록 반영 + 안내 메시지. */
    private fun finishLinkAction(identities: List<LinkedIdentity>?, message: LinkMessage) {
        _link.update { it.copy(busyProvider = null, identities = identities ?: it.identities, message = message) }
    }

    fun clearLinkMessage() = _link.update { it.copy(message = null) }

    fun refreshBalance() {
        sessionScope.launch {
            runCatching { bffApi.getCreditBalance() }
                .onSuccess { resp -> creditStore.setBalance(userSession.current(), resp.balance) }
        }
    }

    /** 메뉴 진입 시 호출 — 오늘 광고 보상 남은 횟수/상한을 BFF 에서 가져와 버튼 상태에 반영. */
    fun refreshAdRewardStatus() {
        sessionScope.launch {
            _adReward.update { it.copy(result = AdRewardResult.None) } // 재진입 시 이전 안내 리셋
            fetchAdRewardStatus()
        }
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
        sessionScope.launch {
            _adReward.update { it.copy(watching = true, result = AdRewardResult.None) }
            var result = AdRewardResult.None
            try {
                val before = creditStore.balance.value
                when (
                    runCatching { rewardedAdController.showRewardedAd(userSession.current()) }
                        .getOrElse { RewardedAdOutcome.UNAVAILABLE }
                ) {
                    // 시청 완료 — 서버 SSV 가 지급. 폴링으로 증가를 확인하면 Granted, 창 안에
                    // 못 보면 Pending("곧 반영") 으로 안내해 무보상 침묵을 막는다.
                    RewardedAdOutcome.REWARD_EARNED ->
                        result = if (pollBalanceUntilIncreased(before)) AdRewardResult.Granted
                        else AdRewardResult.Pending
                    RewardedAdOutcome.UNAVAILABLE -> result = AdRewardResult.NoAd
                    RewardedAdOutcome.DISMISSED -> Unit // 보상 전 닫음 — 조용히 종료(None 유지)
                }
                fetchAdRewardStatus()
            } finally {
                _adReward.update { it.copy(watching = false, result = result) }
            }
        }
    }

    /**
     * SSV 는 Google→BFF 서버간 비동기라 시청 직후 잔액이 아직 안 올랐을 수 있다. 최대 ~6초 동안
     * 1초 간격으로 폴링해 증가를 감지하면 true, 끝내 못 감지하면 false (호출자가 "곧 반영" 안내).
     * 어느 쪽이든 다음 refreshBalance 에서 최종 반영된다.
     */
    private suspend fun pollBalanceUntilIncreased(before: Int): Boolean {
        val attempts = 6
        repeat(attempts) { attempt ->
            val resp = runCatching { bffApi.getCreditBalance() }.getOrNull()
            if (resp != null) {
                creditStore.setBalance(userSession.current(), resp.balance)
                if (resp.balance > before) return true
            }
            if (attempt < attempts - 1) delay(1_000) // 마지막 시도 뒤에는 불필요한 대기 생략
        }
        return false
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
