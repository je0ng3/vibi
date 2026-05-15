package com.vibi.cmp.ui.timeline.sounddeck

import com.vibi.shared.domain.model.BgmClip
import com.vibi.shared.domain.model.SeparationDirective
import com.vibi.shared.domain.model.Stem
import com.vibi.shared.ui.timeline.stemDisplayLabelFromId

/**
 * SoundDeck 카드 한 장의 표현 모델. ViewModel 의 `separationDirectives.selections` 와
 * `bgmClips` 를 평탄화해서 만든다. UI 는 본 모델 리스트만 보고 그린다.
 */
data class SoundCardModel(
    val key: String,
    val label: String,
    val kind: SoundCardKind,
    val source: SoundCardSource,
    val selected: Boolean,
    val volume: Float,
    val audioUrl: String?,
    val rangeStartMs: Long?,
    val rangeEndMs: Long?,
)

enum class SoundCardKind { SPEAKER, VOICE_ALL, BACKGROUND, OTHER_STEM, BGM }

sealed class SoundCardSource {
    data class SeparationStem(val directiveId: String, val stemId: String) : SoundCardSource()
    data class Bgm(val clipId: String) : SoundCardSource()
}

private fun stemKindFor(stemId: String): SoundCardKind = when (Stem.kindFromId(stemId)) {
    com.vibi.shared.domain.model.StemKind.BACKGROUND -> SoundCardKind.BACKGROUND
    com.vibi.shared.domain.model.StemKind.VOICE_ALL -> SoundCardKind.VOICE_ALL
    com.vibi.shared.domain.model.StemKind.SPEAKER -> SoundCardKind.SPEAKER
    com.vibi.shared.domain.model.StemKind.UNKNOWN -> SoundCardKind.OTHER_STEM
}

private fun kindOrder(k: SoundCardKind): Int = when (k) {
    SoundCardKind.SPEAKER -> 0
    SoundCardKind.VOICE_ALL -> 1
    SoundCardKind.BACKGROUND -> 2
    SoundCardKind.OTHER_STEM -> 3
    SoundCardKind.BGM -> 4
}

fun buildSoundDeck(
    separations: List<SeparationDirective>,
    bgmClips: List<BgmClip>,
): List<SoundCardModel> {
    val stemCards = separations.flatMap { dir ->
        dir.selections.map { sel ->
            SoundCardModel(
                key = "stem:${dir.id}:${sel.stemId}",
                label = stemDisplayLabelFromId(sel.stemId),
                kind = stemKindFor(sel.stemId),
                source = SoundCardSource.SeparationStem(dir.id, sel.stemId),
                selected = sel.selected,
                volume = sel.volume,
                audioUrl = sel.audioUrl,
                rangeStartMs = dir.rangeStartMs,
                rangeEndMs = dir.rangeEndMs,
            )
        }
    }
    val bgmCards = bgmClips.map { bgm ->
        SoundCardModel(
            key = "bgm:${bgm.id}",
            label = "삽입한 음원",
            kind = SoundCardKind.BGM,
            source = SoundCardSource.Bgm(bgm.id),
            selected = bgm.volumeScale > 0f,
            volume = bgm.volumeScale,
            audioUrl = bgm.sourceUri,
            rangeStartMs = bgm.startMs,
            rangeEndMs = bgm.startMs + bgm.effectiveDurationMs,
        )
    }
    return (stemCards + bgmCards).sortedWith(
        compareBy({ kindOrder(it.kind) }, { it.label })
    )
}
