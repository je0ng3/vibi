package com.vibi.shared.domain.usecase.save

/**
 * 사용자가 저장/공유 picker sheet 에서 선택할 한 변종의 메타데이터.
 *
 * 자막/더빙 제거 후 사실상 단일 variant (원본 영상) 만 존재. 향후 확장 여지를 위해 모델은 유지.
 */
data class ExportVariant(
    val key: String,
    val kind: ExportVariantKind,
    val displayLabel: String,
) {
    companion object {
        const val KEY_ORIGINAL: String = "original"
    }
}

enum class ExportVariantKind {
    ORIGINAL,
}
