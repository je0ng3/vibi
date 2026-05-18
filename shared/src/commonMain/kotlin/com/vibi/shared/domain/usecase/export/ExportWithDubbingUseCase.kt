package com.vibi.shared.domain.usecase.export

import com.vibi.shared.domain.model.BgmClip
import com.vibi.shared.domain.model.DubClip
import com.vibi.shared.domain.model.SeparationDirective

class ExportWithDubbingUseCase constructor(
    private val ffmpegExecutor: FfmpegExecutor
) {

    suspend fun execute(
        segments: List<SegmentInput>,
        dubClips: List<DubClip>,
        outputPath: String,
        frame: FrameInput? = null,
        bgmClips: List<BgmClip> = emptyList(),
        audioOverridePath: String? = null,
        separationDirectives: List<SeparationDirective> = emptyList(),
        preUploadedInputId: String? = null,
        resolveAudioPath: suspend (audioUri: String) -> String? = { null },
        onProgress: (percent: Int) -> Unit
    ): Result<String> {
        require(segments.isNotEmpty()) { "segments must not be empty" }

        val mixInputs = dubClips.map { clip ->
            DubClipMixInput(
                audioFilePath = clip.audioFilePath,
                startMs = clip.startMs,
                volume = clip.volume
            )
        }

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

        return ffmpegExecutor.renderProject(
            segments = segments,
            dubClips = mixInputs,
            outputPath = outputPath,
            frame = frame,
            bgmClips = bgmMixInputs,
            audioOverridePath = audioOverridePath,
            separationDirectives = separationInputs,
            preUploadedInputId = preUploadedInputId,
            onProgress = onProgress
        )
    }
}
