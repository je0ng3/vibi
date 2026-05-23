package com.vibi.shared.domain.usecase.save

import com.vibi.shared.domain.model.SegmentType
import com.vibi.shared.domain.model.isProjectEdited
import com.vibi.shared.domain.repository.BgmClipRepository
import com.vibi.shared.domain.repository.EditProjectRepository
import com.vibi.shared.domain.repository.SegmentRepository
import com.vibi.shared.domain.repository.SeparationDirectiveRepository
import com.vibi.shared.domain.usecase.share.GallerySaver
import com.vibi.shared.platform.currentTimeMillis
import com.vibi.shared.ui.export.ExportPlatformAdapter
import com.vibi.shared.ui.export.ExportRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 저장 흐름 — 무편집이면 원본 sourceUri 그대로 갤러리 저장, 편집 있으면 BFF render 후 결과 저장.
 *
 * "편집됨" 판정은 도메인 헬퍼 [isProjectEdited] 가 SSOT — trim/volume/speed/BGM/separation/frame/
 * scale/offset/background 까지 정확히 다룸 (trimEndMs == durationMs sentinel 등 포함).
 *
 * textOverlay / imageClip 는 BFF render 파이프라인이 처리하지 않으므로 (preview 전용) 저장에 영향
 * 없음. 사용자가 추가한 overlay 는 갤러리 결과물에 burn 되지 않는다 — UI 단에서 명시 필요.
 */
class SaveAllVariantsUseCase(
    private val platformAdapter: ExportPlatformAdapter,
    private val gallerySaver: GallerySaver,
    private val editProjectRepository: EditProjectRepository,
    private val segmentRepository: SegmentRepository,
    private val bgmClipRepository: BgmClipRepository,
    private val separationDirectiveRepository: SeparationDirectiveRepository,
) {

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        projectId: String,
        onProgress: (percent: Int) -> Unit,
        saveToGallery: Boolean = true,
    ): Result<List<SavedVariant>> {
        return try {
            onProgress(0)
            val project = editProjectRepository.getProject(projectId)
                ?: error("Project not found: $projectId")
            val segments = segmentRepository.getByProjectId(projectId)
            require(segments.isNotEmpty()) { "Project has no segments" }

            val bgmClips = bgmClipRepository.observeClips(projectId).first()
            val separationDirectives = separationDirectiveRepository.getByProject(projectId)

            val firstSeg = segments[0]
            // bypass 조건: 무편집 + 단일 VIDEO segment. sourceUri 형식 변환은 gallerySaver /
            // shareSheetLauncher 가 platform-side resolver 로 처리하므로 가드 불필요.
            val canBypass = segments.size == 1 &&
                firstSeg.type == SegmentType.VIDEO &&
                !isProjectEdited(project, segments, bgmClips, separationDirectives)

            val renderedPath: String = if (canBypass) {
                onProgress(100)
                firstSeg.sourceUri
            } else {
                val request = ExportRequest(
                    projectId = "$projectId#${ExportVariant.KEY_ORIGINAL}",
                    segments = segments,
                    bgmClips = bgmClips,
                    separationDirectives = separationDirectives,
                    frameWidth = project.frameWidth,
                    frameHeight = project.frameHeight,
                    backgroundColorHex = project.backgroundColorHex,
                )
                platformAdapter.executeExport(request) { p ->
                    onProgress((p.coerceIn(0, 100) * 90 / 100))
                }.getOrElse { e ->
                    if (e is CancellationException) throw e
                    error("Render failed: ${e.message}")
                }
            }

            if (saveToGallery) {
                // hashCode + ms timestamp + UUID prefix — race / collision / 동일 ms 재호출 모두 안전.
                val displayName = "VID_${projectId.hashCode().toUInt()}_${currentTimeMillis()}_${Uuid.random().toString().take(8)}"
                gallerySaver.saveVideo(renderedPath, displayName).getOrElse { e ->
                    if (e is CancellationException) throw e
                    error("Gallery save failed: ${e.message}")
                }
            }
            onProgress(100)
            Result.success(listOf(SavedVariant(languageCode = ExportVariant.KEY_ORIGINAL, outputPath = renderedPath)))
        } catch (e: CancellationException) {
            // 구조적 동시성 보존 — viewModelScope cancel 시 caller 가 알아야 함.
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}

data class SavedVariant(
    val languageCode: String,
    val outputPath: String,
)
