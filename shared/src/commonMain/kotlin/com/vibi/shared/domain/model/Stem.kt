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
        const val STEM_ID_BACKGROUND = "background"
        const val STEM_ID_VOICE_ALL = "voice_all"
        private const val SPEAKER_PREFIX = "speaker_"

        fun kindFromId(stemId: String): StemKind = when (stemId) {
            STEM_ID_BACKGROUND -> StemKind.BACKGROUND
            STEM_ID_VOICE_ALL -> StemKind.VOICE_ALL
            else -> if (stemId.startsWith(SPEAKER_PREFIX)) StemKind.SPEAKER else StemKind.UNKNOWN
        }

        fun speakerIndexFromId(stemId: String): Int? {
            if (!stemId.startsWith(SPEAKER_PREFIX)) return null
            return stemId.removePrefix(SPEAKER_PREFIX).toIntOrNull()
        }
    }
}
