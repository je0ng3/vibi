package com.vibi.shared.domain.usecase.save

import com.vibi.shared.data.remote.api.BffApi
import com.vibi.shared.domain.model.SegmentType
import com.vibi.shared.domain.repository.BgmClipRepository
import com.vibi.shared.domain.repository.EditProjectRepository
import com.vibi.shared.domain.repository.SegmentRepository
import com.vibi.shared.domain.repository.SeparationDirectiveRepository
import com.vibi.shared.domain.usecase.share.GallerySaver
import com.vibi.shared.ui.export.ExportPlatformAdapter
import com.vibi.shared.ui.export.ExportRequest
import kotlinx.coroutines.flow.first

/**
 * 저장 흐름 — 자막/더빙 제거 후 단일 variant (원본 영상) 만 처리.
 *
 * 1. 편집 없음 (BGM 0, separation 0, segment 1 + trim 0) → 원본 파일 그대로 갤러리 저장.
 * 2. 편집 있음 → BFF render 후 결과 갤러리 저장.
 */
class SaveAllVariantsUseCase(
    private val platformAdapter: ExportPlatformAdapter,
    private val gallerySaver: GallerySaver,
    private val editProjectRepository: EditProjectRepository,
    private val segmentRepository: SegmentRepository,
    private val bgmClipRepository: BgmClipRepository,
    private val separationDirectiveRepository: SeparationDirectiveRepository,
    @Suppress("UNUSED_PARAMETER") private val bffApi: BffApi,
) {

    suspend operator fun invoke(
        projectId: String,
        onProgress: (percent: Int) -> Unit,
        saveToGallery: Boolean = true,
        @Suppress("UNUSED_PARAMETER") selectedVariantKeys: Set<String>? = null,
    ): Result<List<SavedVariant>> = runCatching {
        val project = editProjectRepository.getProject(projectId)
            ?: error("Project not found: $projectId")
        val segments = segmentRepository.getByProjectId(projectId)
        require(segments.isNotEmpty()) { "Project has no segments" }

        val bgmClips = bgmClipRepository.observeClips(projectId).first()
        val separationDirectives = separationDirectiveRepository.getByProject(projectId)

        val noEdits = bgmClips.isEmpty() &&
            separationDirectives.isEmpty() && segments.size == 1 &&
            segments[0].trimStartMs == 0L && segments[0].trimEndMs == 0L

        val renderedPath: String = if (noEdits && segments[0].type == SegmentType.VIDEO) {
            onProgress(90)
            segments[0].sourceUri
        } else {
            val request = ExportRequest(
                projectId = "$projectId#${ExportVariant.KEY_ORIGINAL}",
                outputLanguageCode = ExportVariant.KEY_ORIGINAL,
                segments = segments,
                bgmClips = bgmClips,
                separationDirectives = separationDirectives,
                frameWidth = project.frameWidth,
                frameHeight = project.frameHeight,
                backgroundColorHex = project.backgroundColorHex,
                preUploadedInputId = null,
            )
            platformAdapter.executeExport(request) { p ->
                onProgress((p.coerceIn(0, 100) * 90 / 100))
            }.getOrElse { e ->
                error("Render failed: ${e.message}")
            }
        }

        if (saveToGallery) {
            val displayName = "VID_${projectId.hashCode().toUInt()}"
            gallerySaver.saveVideo(renderedPath, displayName).getOrElse { e ->
                error("Gallery save failed: ${e.message}")
            }
        }
        onProgress(100)
        listOf(SavedVariant(languageCode = ExportVariant.KEY_ORIGINAL, outputPath = renderedPath))
    }
}

data class SavedVariant(
    val languageCode: String,
    val outputPath: String,
)
