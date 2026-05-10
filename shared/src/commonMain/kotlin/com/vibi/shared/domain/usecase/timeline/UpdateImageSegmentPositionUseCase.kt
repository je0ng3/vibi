package com.vibi.shared.domain.usecase.timeline

import com.vibi.shared.domain.model.SegmentType
import com.vibi.shared.domain.repository.SegmentRepository

class UpdateImageSegmentPositionUseCase constructor(
    private val segmentRepository: SegmentRepository
) {
    suspend operator fun invoke(
        segmentId: String,
        xPct: Float,
        yPct: Float,
        widthPct: Float,
        heightPct: Float
    ) {
        val segment = segmentRepository.getSegment(segmentId)
            ?: throw IllegalArgumentException("Segment not found: $segmentId")
        require(segment.type == SegmentType.IMAGE) {
            "Position can only be set on IMAGE segments"
        }
        segmentRepository.updateSegment(
            segment.copy(
                imageXPct = xPct.coerceIn(0f, 100f),
                imageYPct = yPct.coerceIn(0f, 100f),
                imageWidthPct = widthPct.coerceIn(5f, 100f),
                imageHeightPct = heightPct.coerceIn(5f, 100f)
            )
        )
    }
}
