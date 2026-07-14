package com.vibi.shared.ui.timeline

import com.vibi.shared.domain.model.SeparationCost
import com.vibi.shared.domain.model.Stem

enum class AudioSeparationStep {
    SETUP,
    PROCESSING,
    PICK_STEMS,
    DONE,
    FAILED
}

data class StemSelectionUi(
    val stemId: String,
    val selected: Boolean,
    val volume: Float
)

/**
 * BGM "배경음 제거" 첫 분리 confirmation prompt. clipId 별로 1개씩만 활성 (한 번에 한 dialog).
 * costPreview null = fetch 미완료 ("Checking balance…" placeholder), 도착 후 UI 가 자동 갱신.
 * 이미 voice-only 캐시가 있어 restore↔isolate 토글하는 경우엔 prompt 띄우지 않음 (재분리 없음).
 */
data class BgmRemovalCostPrompt(
    val clipId: String,
    val durationMs: Long,
    val costPreview: CreditCostPreview? = null,
)

/**
 * 분리 시작 전 비용 미리보기. BFF `/credits/cost` 응답 도메인 모델([SeparationCost])을 그대로 UI 가
 * 들고 있다 — 필드가 동일해 별도 매핑 없이 재사용. 부족하면 UI 가 Start 버튼 disable + "충전 필요"
 * 분기를 한다. null = 아직 fetch 안 됨 (sheet 막 열린 직후 짧은 window — Start 버튼 "Loading…" 표시).
 */
typealias CreditCostPreview = SeparationCost

data class AudioSeparationUiState(
    val segmentId: String,
    val step: AudioSeparationStep = AudioSeparationStep.SETUP,
    val numberOfSpeakers: Int = 2,
    val jobId: String? = null,
    val progress: Int = 0,
    val progressReason: String? = null,
    val stems: List<Stem> = emptyList(),
    val selections: Map<String, StemSelectionUi> = emptyMap(),
    val muteOriginalSegmentAudio: Boolean = true,
    val errorMessage: String? = null,
    /** 사용자 지정 부분 구간 (range mode 진입). null = 영상 전체. */
    val rangeStartMs: Long? = null,
    val rangeEndMs: Long? = null,
    /**
     * 사용자가 진행 중 sheet 를 명시적으로 닫았는지 — 이후 FAILED 가 도착해도 sheet 자동
     * 재오픈 안 함. directive 막대만으로 알림. 새 분리 시작(onShowAudioSeparationSheet 통한
     * 재진입) 시 자연스럽게 false 로 리셋됨.
     */
    val userDismissed: Boolean = false,
    /**
     * SETUP 단계에서 BFF /credits/cost 로 미리 받아둔 비용/잔액 정보. null = fetch 아직 안 됨
     * (sheet 막 열린 직후 / fetch 실패 시 fallback). UI 가 "X 크레딧 사용" 표시 + 부족 시
     * Start 분기에 사용. FAILED 단계에선 [insufficientCredits] 와 함께 표시.
     */
    val costPreview: CreditCostPreview? = null,
    /**
     * FAILED 단계에서 사유가 잔액 부족인지. true 면 UI 가 "충전 필요" 메시지 + Buy credits
     * 버튼 노출. 일반 실패 (네트워크/Perso 에러) 와 분리 — 사용자에게 정확한 다음 단계 안내.
     */
    val insufficientCredits: Boolean = false,
) {
    val canStart: Boolean
        get() = step == AudioSeparationStep.SETUP &&
            numberOfSpeakers in 1..10 &&
            // costPreview 가 still null 이면 일단 허용 (fetch 미완료 사이의 startup race — BFF 가
            // 권위 검증 후 402 매핑되어 충전 UI 로 자연 폴백). sufficient=false 가 명시되면 차단.
            (costPreview?.sufficient != false)

    val canMix: Boolean
        get() = step == AudioSeparationStep.PICK_STEMS &&
            selections.values.any { it.selected }
}

/**
 * BFF progressReason values come in English (Enqueue Pending / Transcribing / ...).
 * Map them to Korean labels for the progress UI, falling back to the raw string
 * when the BFF adds a reason we haven't seen yet.
 */
fun localizeProgressReason(reason: String?): String = when (reason) {
    null, "" -> "Preparing"
    "Enqueue Pending" -> "Queued"
    "Slow Mode Pending" -> "Queued (slow mode)"
    "Uploading" -> "Uploading"
    "Transcribing" -> "Transcribing"
    "Translating" -> "Translating"
    "Generating Voice" -> "Separating audio"
    "Completed" -> "Done"
    "Failed" -> "Failed"
    else -> reason
}

/**
 * Display label for a stem. 배경음은 2종 — 순수 BGM(`background`)과 리액션 포함(`background_reaction`) —
 * 이라 라벨로 구분한다. Per-speaker indices are 0-based on the wire; show them as 1-based to match
 * the user's mental model.
 */
fun stemDisplayLabel(stem: Stem): String = when {
    stem.stemId == Stem.STEM_ID_BACKGROUND -> "Background (no reaction)"
    stem.stemId == Stem.STEM_ID_BACKGROUND_REACTION -> "Background (with reaction)"
    stem.stemId == Stem.STEM_ID_VOICE_ALL -> "All speakers"
    stem.speakerIndex != null -> "Speaker ${stem.speakerIndex + 1}"
    else -> stem.label.ifBlank { stem.stemId }
}

/**
 * [stemDisplayLabel] 의 stemId-only 변종 — directive 의 [com.vibi.shared.domain.repository.StemSelection]
 * 처럼 Stem 객체가 없는 경로에서 사용. label 폴백이 stemId 본인.
 */
fun stemDisplayLabelFromId(stemId: String): String = when {
    stemId == Stem.STEM_ID_BACKGROUND -> "Background (no reaction)"
    stemId == Stem.STEM_ID_BACKGROUND_REACTION -> "Background (with reaction)"
    stemId == Stem.STEM_ID_VOICE_ALL -> "All speakers"
    else -> Stem.speakerIndexFromId(stemId)?.let { "Speaker ${it + 1}" } ?: stemId
}

/**
 * 주어진 stem 집합에 대해 기본 mix 선택될 stemId 들을 계산한다.
 *
 * - voice_all("모든 화자")은 화자별 stem 과 중복이라 제외.
 * - 배경음은 상호 배타 — 순수 배경음(background)을 우선 선택하고, **그것이 없을 때만** 리액션 포함
 *   (background_reaction)을 선택한다. 순수 배경음이 함께 오면 background_reaction 은 기본 음소거.
 * - 그 외(SPEAKER/OTHER)는 선택.
 *
 * 집합 전체를 봐야 하는 이유: `background_reaction` 만 있고 순수 `background` 가 없는 잡을
 * 무조건 음소거하면 선택 stem 이 0개가 돼, 정상 잡이 "선택 없음" 가드에 걸려 FAILED 로 처리된다.
 */
fun defaultSelectedStemIds(stemIds: Collection<String>): Set<String> {
    val hasPureBackground = Stem.STEM_ID_BACKGROUND in stemIds
    return stemIds.filterTo(mutableSetOf()) { id ->
        when (id) {
            Stem.STEM_ID_VOICE_ALL -> false
            Stem.STEM_ID_BACKGROUND_REACTION -> !hasPureBackground
            else -> true
        }
    }
}
