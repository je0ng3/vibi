package com.vibi.shared.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Asset-by-reference render payload — segment 영상/BGM 의 R2 키만 담아 `/api/v2/render/v3`
 * 에 JSON 으로 전송. BFF 가 R2 에서 다운로드 후 ffmpeg.
 *
 * BFF 측 DTO ([com.vibi.bff.model.RenderConfigV3]) 와 1:1 — 필드 추가 시 양쪽 갱신 필수.
 */
@Serializable
data class RenderConfigV3(
    val segments: List<RenderSegmentV3>,
    val bgmClips: List<RenderBgmClipV3> = emptyList(),
    val separationDirectives: List<RenderSeparationDirective> = emptyList(),
    /** "video" (mp4) | "audio" (m4a). */
    val outputKind: String = "video",
    /** "high" | "medium" | "low". audio 모드는 무시. */
    val quality: String = "medium",
)

@Serializable
data class RenderSegmentV3(
    /** R2 asset key — `assets/<sha256hex>.<ext>`. AssetUploadManager 가 발급. */
    val sourceAssetKey: String,
    val order: Int,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val volumeScale: Float = 1.0f,
    val speedScale: Float = 1.0f,
)

@Serializable
data class RenderBgmClipV3(
    val audioAssetKey: String,
    val startMs: Long,
    val volume: Float = 1.0f,
    val sourceTrimStartMs: Long = 0L,
    val sourceTrimEndMs: Long = 0L,
)
