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
 * `RenderRepository` 의 commonMain 구현. 자막/더빙/분리 가 source 로 쓸 "편집 영상" 1개만 만든다.
 *
 * 제외:
 *  - dubClips / audioOverridePath: 더빙은 본 source 의 audio 를 STT 후 재합성.
 *
 * 포함 (사용자가 timeline 에 만든 결과를 STT 가 듣게):
 *  - segments (trim/speed/volume)
 *  - frame (캔버스 비율 / 배경)
 *  - bgmClips: 본 BG 음악이 STT 에 영향. 포함해야 사용자 의도 반영.
 *  - separationDirectives: 음원분리 결과 stem mix — 본 source 에 반영.
 *
 * bgmClips 는 플랫폼별 path 해결이 필요 (Android `content://` URI). 본 구현은 기본적으로 path
 * 해결 안 함 — 따라서 BGM 이 필요한 프로젝트는 platform-specific resolver 를 주입해야 함.
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
            dubClips = emptyList(),
            frame = frame,
            bgmClips = bgmMixInputs,
            audioOverridePath = null,
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
