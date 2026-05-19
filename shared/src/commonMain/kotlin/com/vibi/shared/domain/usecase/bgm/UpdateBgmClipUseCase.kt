package com.vibi.shared.domain.usecase.bgm

import com.vibi.shared.domain.model.BgmClip
import com.vibi.shared.domain.repository.BgmClipRepository

class UpdateBgmClipUseCase constructor(
    private val bgmClipRepository: BgmClipRepository
) {
    suspend operator fun invoke(
        clipId: String,
        startMs: Long? = null,
        volumeScale: Float? = null,
        speedScale: Float? = null,
        sourceTrimStartMs: Long? = null,
        sourceTrimEndMs: Long? = null,
    ): BgmClip {
        val current = bgmClipRepository.getClip(clipId)
            ?: throw IllegalArgumentException("BGM clip not found: $clipId")
        startMs?.let { require(it >= 0L) { "startMs must be >= 0: $it" } }
        sourceTrimStartMs?.let { require(it >= 0L) { "sourceTrimStartMs must be >= 0: $it" } }
        sourceTrimEndMs?.let { require(it >= 0L) { "sourceTrimEndMs must be >= 0: $it" } }
        val nextTrimStart = sourceTrimStartMs ?: current.sourceTrimStartMs
        val nextTrimEnd = sourceTrimEndMs ?: current.sourceTrimEndMs
        // trim range 가 source 길이 안에 있고 start < end 인지 검증. end==0L 은 "끝까지" 기본값.
        if (nextTrimEnd > 0L) {
            require(nextTrimEnd <= current.sourceDurationMs) {
                "sourceTrimEndMs($nextTrimEnd) must be <= sourceDurationMs(${current.sourceDurationMs})"
            }
            require(nextTrimStart < nextTrimEnd) {
                "sourceTrimStartMs($nextTrimStart) must be < sourceTrimEndMs($nextTrimEnd)"
            }
        }
        val updated = current.copy(
            startMs = startMs ?: current.startMs,
            volumeScale = volumeScale?.coerceIn(BgmClip.MIN_VOLUME, BgmClip.MAX_VOLUME)
                ?: current.volumeScale,
            speedScale = speedScale?.coerceIn(BgmClip.MIN_SPEED, BgmClip.MAX_SPEED)
                ?: current.speedScale,
            sourceTrimStartMs = nextTrimStart,
            sourceTrimEndMs = nextTrimEnd,
        )
        bgmClipRepository.updateClip(updated)
        return updated
    }
}
