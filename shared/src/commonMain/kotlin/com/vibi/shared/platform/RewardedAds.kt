package com.vibi.shared.platform

/**
 * 보상형 광고 컨트롤러 — UI/ViewModel 이 플랫폼 무관하게 호출. 실제 구현은
 * `androidMain`([com.vibi.shared.platform.AndroidRewardedAdController], Google Mobile Ads SDK) /
 * `iosMain`([com.vibi.shared.platform.IosRewardedAdController], Swift `RewardedAdBridge`).
 *
 * **보상은 클라가 지급하지 않는다.** 광고 로드 시 SSV userId 를 심어두면, 시청 완료 시 Google 이
 * BFF `/credits/admob-ssv` 로 서명된 콜백을 보내 +1 크레딧을 지급한다. 따라서 이 함수는 "사용자가
 * 끝까지 봤는가"([RewardedAdOutcome.REWARD_EARNED]) 만 알려주고, 호출자는 잔액을 새로고침한다.
 */
interface RewardedAdController {
    /**
     * 보상형 광고를 로드+표시한다. [userId] 는 SSV ServerSideVerificationOptions 에 실려 BFF
     * 콜백의 `user_id` 가 된다 — BFF 가 이 사용자에게 크레딧을 귀속.
     */
    suspend fun showRewardedAd(userId: String): RewardedAdOutcome
}

enum class RewardedAdOutcome {
    /** 끝까지 시청해 보상 조건 충족. 실제 크레딧은 BFF SSV 가 지급하므로 호출자는 잔액 새로고침. */
    REWARD_EARNED,

    /** 보상 전 닫음 / 취소. */
    DISMISSED,

    /** 로드 실패·미준비·광고 없음·SDK 미초기화 등. */
    UNAVAILABLE,
}

/**
 * Swift 가 구현해 K/N 으로 주입하는 보상형 광고 bridge (iOS 전용). Android 는 동일 역할을
 * [AndroidRewardedAdController] 가 Google Mobile Ads SDK 로 직접 수행한다 (bridge 불필요).
 *
 * `GADRewardedAd` 는 Swift-only API 라 Kotlin/Native cinterop 으로 직접 호출할 수 없다 —
 * [IapBridge] 와 동일한 callback bridge 방식. [IosRewardedAdController] 가 suspend 로 wrap 한다.
 *
 * outcome 문자열 단일 source — Swift 와 Kotlin 양쪽이 같은 값 사용 (변경 시 두 쪽 모두 갱신):
 * `"earned"` | `"dismissed"` | `"unavailable"`.
 */
interface RewardedAdBridge {
    /**
     * 보상형 광고 로드+표시. callback 은 정확히 1회 호출.
     *
     * @param userId SSV `GADServerSideVerificationOptions.userIdentifier` 로 설정 → BFF 콜백 user_id.
     * @param callback outcome `"earned"` | `"dismissed"` | `"unavailable"`.
     */
    fun showRewardedAd(userId: String, callback: (outcome: String) -> Unit)
}
