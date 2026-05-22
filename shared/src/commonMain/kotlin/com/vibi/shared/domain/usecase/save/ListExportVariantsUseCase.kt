package com.vibi.shared.domain.usecase.save

import com.vibi.shared.domain.repository.EditProjectRepository

/**
 * 저장/공유 picker sheet 가 노출할 variant 목록을 계산.
 *
 * 현재는 원본 영상 단일 variant 만 — 자막/더빙 variant 제거 후 picker 무의미해졌으나,
 * 호출부 (TimelineViewModel) 시그니처 유지를 위해 single-entry 리스트 반환.
 */
class ListExportVariantsUseCase(
    private val editProjectRepository: EditProjectRepository,
) {

    suspend operator fun invoke(projectId: String): Result<List<ExportVariant>> = runCatching {
        editProjectRepository.getProject(projectId) ?: error("Project not found: $projectId")
        listOf(
            ExportVariant(
                key = ExportVariant.KEY_ORIGINAL,
                kind = ExportVariantKind.ORIGINAL,
                displayLabel = "원본 영상",
            )
        )
    }
}
