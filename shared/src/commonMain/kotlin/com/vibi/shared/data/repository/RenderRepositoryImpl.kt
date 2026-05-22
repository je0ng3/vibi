package com.vibi.shared.data.repository

import com.vibi.shared.domain.model.BgmClip
import com.vibi.shared.domain.model.EditProject
import com.vibi.shared.domain.model.Segment
import com.vibi.shared.domain.model.SeparationDirective
import com.vibi.shared.domain.repository.RenderRepository
import com.vibi.shared.domain.usecase.render.RenderKind
import com.vibi.shared.domain.usecase.export.BgmClipMixInput
import com.vibi.shared.domain.usecase.export.FrameInput
import com.vibi.shared.domain.usecase.export.SegmentInput
import com.vibi.shared.domain.usecase.export.toExportInput

/**
 * `RenderRepository` 의 commonMain 구현. 음원분리 가 source 로 쓸 "편집 영상" 1개만 만든다.
 *
 * 포함:
 *  - segments (trim/speed/volume)
 *  - frame (캔버스 비율 / 배경)
 *  - bgmClips
 *  - separationDirectives: 음원분리 결과 stem mix
 */
class RenderRepositoryImpl(
    private val executor: RemoteRenderExecutor,
    /**
     * `content://` 또는 `file://` URI 를 절대 경로로 변환. null 을 반환하면 해당 클립 skip.
     * 기본 구현은 항상 null (BGM 미포함).
     */
    private val resolveAudioPath: suspend (uri: String) -> String? = { null },
) : RenderRepository {

    override suspend fun submitForEditedSource(
        project: EditProject,
        segments: List<Segment>,
        bgmClips: List<BgmClip>,
        separationDirectives: List<SeparationDirective>,
        kind: RenderKind,
        onProgress: (percent: Int) -> Unit,
    ): Result<String> {
        require(segments.isNotEmpty()) { "Cannot render: project has no segments" }

        val segmentInputs = segments.map { seg ->
            SegmentInput(
                sourceFilePath = seg.sourceUri,
                type = seg.type,
                order = seg.order,
                durationMs = seg.durationMs,
                trimStartMs = seg.trimStartMs,
                trimEndMs = seg.trimEndMs,
                width = seg.width,
                height = seg.height,
                imageXPct = seg.imageXPct,
                imageYPct = seg.imageYPct,
                imageWidthPct = seg.imageWidthPct,
                imageHeightPct = seg.imageHeightPct,
                volumeScale = seg.volumeScale,
                speedScale = seg.speedScale,
            )
        }

        val firstSegment = segmentInputs.minByOrNull { it.order } ?: segmentInputs.first()
        val frame = if (project.frameWidth > 0 && project.frameHeight > 0) {
            FrameInput(
                width = project.frameWidth,
                height = project.frameHeight,
                backgroundColorHex = project.backgroundColorHex,
            )
        } else FrameInput(
            width = firstSegment.width,
            height = firstSegment.height,
            backgroundColorHex = project.backgroundColorHex,
        )

        val bgmMixInputs = bgmClips.mapNotNull { clip ->
            val localPath = resolveAudioPath(clip.sourceUri) ?: return@mapNotNull null
            BgmClipMixInput(
                audioFilePath = localPath,
                startMs = clip.startMs,
                volume = clip.volumeScale,
                speed = clip.speedScale,
                sourceTrimStartMs = clip.sourceTrimStartMs,
                sourceTrimEndMs = clip.sourceTrimEndMs,
            )
        }

        val separationInputs = separationDirectives.mapNotNull { it.toExportInput() }

        return executor.submitAndAwaitJobId(
            segments = segmentInputs,
            frame = frame,
            bgmClips = bgmMixInputs,
            separationDirectives = separationInputs,
            preUploadedInputId = null,
            outputKind = when (kind) {
                RenderKind.AUDIO -> "audio"
                RenderKind.VIDEO -> "video"
            },
            onProgress = onProgress,
        )
    }
}
