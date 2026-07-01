package com.vibi.shared.ui.export

import com.vibi.shared.domain.model.Segment
import com.vibi.shared.domain.usecase.export.BgmClipMixInput
import com.vibi.shared.domain.usecase.export.FfmpegExecutor
import com.vibi.shared.domain.usecase.export.FrameInput
import com.vibi.shared.domain.usecase.export.SegmentInput
import com.vibi.shared.domain.usecase.export.toExportInput
import com.vibi.shared.platform.openInputFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android Export 어댑터 — BFF [FfmpegExecutor] (= RemoteRenderExecutor) 에 직접 위임.
 *
 * iOS [com.vibi.shared.ui.export] 의 IosExportPlatformAdapter 와 동일 동작이되, 임시 디렉터리
 * 파일 복사 단계가 없다: Android 의 [readFileBytes] 가 file:// · content:// · 절대경로를 모두
 * 직접 읽으므로 segment/BGM 의 sourceUri 를 그대로 입력 경로로 넘긴다 (RemoteRenderExecutor 가
 * 업로드 직전에 readFileBytes 로 materialize).
 *
 * 에러 정책 (iOS 와 동일):
 *  - BGM 등 path 해소 실패 → fail-loud (silent drop 금지) 지만 raw URI 노출 안 하고 파일명만
 *    surface. 여러 BGM 실패 시 일괄 수집해서 한 번에 throw.
 *  - CancellationException 은 그대로 rethrow (구조적 동시성 보존).
 */
class AndroidExportPlatformAdapter(
    private val executor: FfmpegExecutor,
) : ExportPlatformAdapter {

    override suspend fun executeExport(
        request: ExportRequest,
        onProgress: (percent: Int) -> Unit,
    ): Result<String> {
        return try {
            // readFileBytes 가 file:// · content:// · 절대경로를 직접 처리하므로 임시 복사 없이
            // sourceUri 를 그대로 입력 경로로 전달.
            val segmentInputs = request.segments.map { segment ->
                segment.toInput(segment.sourceUri)
            }

            // RemoteRenderExecutor 는 outputPath 의 디렉터리부는 무시하고 파일명만 사용해
            // (cacheDir 에) 결과를 저장하므로 파일명만 의미 있다.
            val outputPath = "export.mp4"

            val frame = if (request.frameWidth > 0 && request.frameHeight > 0) {
                FrameInput(
                    width = request.frameWidth,
                    height = request.frameHeight,
                    backgroundColorHex = request.backgroundColorHex,
                )
            } else null

            // BGM 실패는 일괄 수집 — 사용자가 retry 사이클을 N번 돌지 않게.
            val bgmInputs = mutableListOf<BgmClipMixInput>()
            val bgmFailures = mutableListOf<String>()
            for (clip in request.bgmClips) {
                if (!isReadable(clip.sourceUri)) {
                    bgmFailures += clip.sourceUri.fileNameOnly()
                    continue
                }
                bgmInputs += BgmClipMixInput(
                    audioFilePath = clip.sourceUri,
                    startMs = clip.startMs,
                    volume = clip.volumeScale,
                    speed = clip.speedScale,
                    sourceTrimStartMs = clip.sourceTrimStartMs,
                    sourceTrimEndMs = clip.sourceTrimEndMs,
                )
            }
            if (bgmFailures.isNotEmpty()) {
                error("BGM unreadable: ${bgmFailures.joinToString(", ")}")
            }

            // directive 의 stem tempo = 앵커된 세그먼트의 speedScale. 세그먼트가 단일 진실원천이라
            // directive 는 speed 를 저장 안 하고 여기서 resolve 해 주입한다. 미앵커(legacy, segmentId
            // 빔)거나 세그먼트 부재면 1.0 (원본 tempo).
            val speedBySegmentId = request.segments.associate { it.id to it.speedScale }
            val directives = request.separationDirectives.mapNotNull { d ->
                val speed = speedBySegmentId[d.segmentId]?.takeIf { it > 0f } ?: 1f
                d.toExportInput(appliedSpeedScale = speed)
            }

            executor.renderProject(
                segments = segmentInputs,
                outputPath = outputPath,
                frame = frame,
                bgmClips = bgmInputs,
                separationDirectives = directives,
                onProgress = onProgress,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * 입력 경로가 실제로 열리는지 검증. 전체를 슬럽하지 않고 InputStream 을 열었다 닫아
     * (content:// · file:// · 절대경로) 저렴하게 확인 — 여기서 통과하면 실제 렌더 read 도 통과한다.
     * 취소는 그대로 전파하고, 그 외 실패만 "읽을 수 없음" 으로 본다.
     */
    private suspend fun isReadable(uri: String): Boolean = try {
        withContext(Dispatchers.IO) { openInputFor(uri).close() }
        true
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        false
    }

    /** URI 의 마지막 path segment만 추출 (raw URI / 토큰 / 식별자 metadata leak 방지). */
    private fun String.fileNameOnly(): String =
        substringAfterLast('/').take(64).ifBlank { "(unknown)" }

    private fun Segment.toInput(localPath: String) = SegmentInput(
        sourceFilePath = localPath,
        type = type,
        order = order,
        durationMs = durationMs,
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        width = width,
        height = height,
        volumeScale = volumeScale,
        speedScale = speedScale,
    )
}
