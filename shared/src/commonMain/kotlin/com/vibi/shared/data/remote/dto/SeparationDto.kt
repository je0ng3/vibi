package com.vibi.shared.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * BFF `/api/v2/separate` multipart `spec` 필드. 클라가 trim + audio extract 까지 끝낸 audio 를
 * file 파트로 보내고, spec 은 메타 (현재는 source language 만) 만 담는다. mediaType / trim window /
 * editedRenderJobId 등 옛 필드는 surface 에서 제거.
 */
@Serializable
data class SeparationSpec(
    val sourceLanguageCode: String = "auto",
)

@Serializable
data class SeparationJobResponse(
    val jobId: String
)

@Serializable
data class StemDto(
    val stemId: String,
    val label: String,
    val url: String
)

@Serializable
data class SeparationStatusResponse(
    val jobId: String,
    val status: String,
    val progress: Int = 0,
    val progressReason: String? = null,
    val stems: List<StemDto> = emptyList(),
    /** status=FAILED 시 사용자 표시용 friendly 안내 문구(BFF 가 sanitize 해 제공 — raw upstream 아님).
     *  클라는 이 문구를 그대로 표시(문구 단일 소스=BFF). */
    val error: String? = null,
    /**
     * status=FAILED 시 사용자 조치 가능 실패의 stable code (예: no_audio_detected / audio_too_short /
     * separation_start_failed). 인프라 오류면 null. 표시는 [error] 로 충분하며, 코드는 향후 클라
     * 분기(재시도 버튼 등)용으로 보존.
     */
    val errorCode: String? = null,
    /**
     * READY 시 BFF 가 ffprobe 로 잰 stem FLAC 의 실측 길이(ms). TimelineViewModel 이
     * SeparationDirective.rangeEndMs 를 사용자 선택값이 아닌 실제 stem 길이로 보정하기 위해 사용.
     * 누락(서버 측정 실패) 시 클라이언트가 사용자 선택값을 그대로 사용 — 기존 동작 fallback.
     */
    val actualDurationMs: Long? = null
)
