package com.vibi.shared.domain.model

enum class StemKind {
    BACKGROUND,
    VOICE_ALL,
    SPEAKER,
    UNKNOWN
}

data class Stem(
    val stemId: String,
    val label: String,
    val url: String,
    val kind: StemKind,
    val speakerIndex: Int? = null
) {
    companion object {
        /** 순수 BGM (리액션 제외). */
        const val STEM_ID_BACKGROUND = "background"

        /** 리액션(효과음·추임새·비주 화자) 포함 배경음. [STEM_ID_BACKGROUND] 와 상호 배타 — 최대 1개만 mix. */
        const val STEM_ID_BACKGROUND_REACTION = "background_reaction"
        const val STEM_ID_VOICE_ALL = "voice_all"
        private const val SPEAKER_PREFIX = "speaker_"

        fun kindFromId(stemId: String): StemKind = when (stemId) {
            STEM_ID_BACKGROUND, STEM_ID_BACKGROUND_REACTION -> StemKind.BACKGROUND
            STEM_ID_VOICE_ALL -> StemKind.VOICE_ALL
            else -> if (stemId.startsWith(SPEAKER_PREFIX)) StemKind.SPEAKER else StemKind.UNKNOWN
        }

        /** 두 배경음 변종 중 하나인지 — 순수 BGM 또는 리액션 포함. 상호 배타 규칙에 사용. */
        fun isBackgroundId(stemId: String): Boolean =
            stemId == STEM_ID_BACKGROUND || stemId == STEM_ID_BACKGROUND_REACTION

        fun speakerIndexFromId(stemId: String): Int? {
            if (!stemId.startsWith(SPEAKER_PREFIX)) return null
            return stemId.removePrefix(SPEAKER_PREFIX).toIntOrNull()
        }
    }
}
