package com.vibi.shared.platform

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * [RewardedAdBridge] (Swift `RewardedAdBridgeImpl`, GoogleMobileAds SDK) 의 callback 을 suspend
 * API 로 wrap — Android [AndroidRewardedAdController] 와 동일한 [RewardedAdController] 계약.
 *
 * 보상 적립은 Google → BFF SSV 콜백이 처리하므로, outcome 은 시청 완료 여부만 전달한다.
 */
class IosRewardedAdController(
    private val bridge: RewardedAdBridge,
) : RewardedAdController {

    override suspend fun showRewardedAd(userId: String): RewardedAdOutcome =
        suspendCancellableCoroutine { cont ->
            bridge.showRewardedAd(userId) { outcome ->
                val result = when (outcome) {
                    "earned" -> RewardedAdOutcome.REWARD_EARNED
                    "dismissed" -> RewardedAdOutcome.DISMISSED
                    else -> RewardedAdOutcome.UNAVAILABLE
                }
                if (cont.isActive) cont.resume(result)
            }
        }
}
