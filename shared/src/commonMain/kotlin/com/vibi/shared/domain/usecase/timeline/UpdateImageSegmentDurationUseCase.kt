package com.vibi.shared.domain.usecase.timeline

import com.vibi.shared.domain.model.SegmentType
import com.vibi.shared.domain.repository.SegmentRepository

class UpdateImageSegmentDurationUseCase constructor(
    private val segmentRepository: SegmentRepository
) {
    suspend operator fun invoke(segmentId: String, durationMs: Long) {
        val segment = segmentRepository.getSegment(segmentId)
            ?: throw IllegalArgumentException("Segment not found: $segmentId")
        require(segment.type == SegmentType.IMAGE) {
            "Duration can only be set on IMAGE segments"
        }
        require(durationMs >= MIN_DURATION_MS) {
            "Duration must be >= ${MIN_DURATION_MS}ms"
        }
        segmentRepository.updateSegment(segment.copy(durationMs = durationMs))
    }

    companion object {
        const val MIN_DURATION_MS = 500L
    }
}
