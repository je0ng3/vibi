package com.vibi.shared.domain.chat

import com.vibi.shared.data.remote.dto.ContextBgmClipDto
import com.vibi.shared.data.remote.dto.ContextDubClipDto
import com.vibi.shared.data.remote.dto.ContextSegmentDto
import com.vibi.shared.data.remote.dto.ContextSeparationDirectiveDto
import com.vibi.shared.data.remote.dto.ContextStemDto
import com.vibi.shared.data.remote.dto.ContextSubtitleClipDto
import com.vibi.shared.data.remote.dto.ProjectContextDto
import com.vibi.shared.domain.model.SegmentType
import com.vibi.shared.ui.timeline.TimelineUiState

/**
 * TimelineUiState → ProjectContextDto 압축. Gemini 가 발화 해석 시 참조하는 단일 source.
 *
 * 토큰 비용 통제:
 *  - 자막 텍스트 200자 cap (BFF 와 동일 정책)
 *  - sourceUri 는 path 마지막 segment 만
 *  - 분리 stems 는 selections 의 flat 화 (label = stem id 그대로)
 */
object ProjectContextBuilder {
    private const val TEXT_CAP = 200

    fun build(state: TimelineUiState): ProjectContextDto {
        val segs = mutableListOf<ContextSegmentDto>()
        var acc = 0L
        for (seg in state.segments) {
            if (seg.type != SegmentType.VIDEO) {
                acc += seg.effectiveDurationMs
                continue
            }
            val start = acc
            val end = acc + seg.effectiveDurationMs
            segs += ContextSegmentDto(
                id = seg.id,
                startMs = start,
                endMs = end,
                sourceUri = seg.sourceUri.substringAfterLast('/').take(64),
                speedScale = seg.speedScale,
                volumeScale = seg.volumeScale,
            )
            acc = end
        }

        val subs = state.subtitleClips.mapIndexed { i, c ->
            ContextSubtitleClipDto(
                id = c.id,
                index = i + 1,
                startMs = c.startMs,
                endMs = c.endMs,
                text = c.text.take(TEXT_CAP),
                languageCode = c.languageCode,
            )
        }

        val dubs = state.dubClips.map { c ->
            ContextDubClipDto(
                id = c.id,
                startMs = c.startMs,
                endMs = c.startMs + c.durationMs,
                voiceId = c.voiceId,
            )
        }

        val bgms = state.bgmClips.map { b ->
            ContextBgmClipDto(
                id = b.id,
                startMs = b.startMs,
                endMs = b.startMs + b.effectiveDurationMs,
                volumeScale = b.volumeScale,
                speedScale = b.speedScale,
            )
        }

        val stems = state.separationDirectives.flatMap { d ->
            d.selections.map { s ->
                ContextStemDto(
                    stemId = s.stemId,
                    label = s.stemId,
                    volume = s.volume,
                    selected = s.selected,
                )
            }
        }

        // directive 단위 (range + numberOfSpeakers) — Gemini 의 중복 분리 거부 / 비용 안내 /
        // 대안 제시(기존 삭제 후 재분리 vs 짧은 분할) 판단용. stems 는 stemId/label 만이라
        // range 정보 없음.
        val directives = state.separationDirectives.map { d ->
            ContextSeparationDirectiveDto(
                id = d.id,
                rangeStartMs = d.rangeStartMs,
                rangeEndMs = d.rangeEndMs,
                durationMs = d.durationMs,
                numberOfSpeakers = d.numberOfSpeakers,
            )
        }

        val selectedClipId = state.selectedDubClipId
            ?: state.selectedSubtitleClipId
            ?: state.selectedImageClipId
            ?: state.selectedBgmClipId

        // 사용자가 UI 로 잡아둔 구간을 함께 전달 — Gemini 가 "이 구간..." 같은 deictic 발화를
        // pendingRange 로 풀 수 있게. range 모드가 아니면 null 로 보내 LLM 이 추정 안 함.
        val (rangeStart, rangeEnd) = if (state.isRangeSelecting &&
            state.pendingRangeEndMs > state.pendingRangeStartMs
        ) {
            state.pendingRangeStartMs to state.pendingRangeEndMs
        } else {
            null to null
        }

        return ProjectContextDto(
            segments = segs,
            subtitleClips = subs,
            dubClips = dubs,
            bgmClips = bgms,
            separationStems = stems,
            separationDirectives = directives,
            currentPlayheadMs = state.playbackPositionMs,
            selectedSegmentId = state.selectedSegmentId,
            selectedClipId = selectedClipId,
            isRangeSelecting = state.isRangeSelecting,
            pendingRangeStartMs = rangeStart,
            pendingRangeEndMs = rangeEnd,
            videoDurationMs = state.videoDurationMs,
        )
    }
}
