package com.vibi.shared.domain.usecase.text

import com.vibi.shared.platform.generateId

import com.vibi.shared.domain.model.TextOverlay
import com.vibi.shared.domain.repository.TextOverlayRepository

class DuplicateTextOverlayUseCase constructor(
    private val textOverlayRepository: TextOverlayRepository
) {
    suspend operator fun invoke(overlayId: String): TextOverlay {
        val source = textOverlayRepository.getOverlay(overlayId)
            ?: throw IllegalArgumentException("Text overlay not found: $overlayId")
        val duration = source.endMs - source.startMs
        val duplicate = source.copy(
            id = generateId(),
            startMs = source.endMs,
            endMs = source.endMs + duration
        )
        textOverlayRepository.addOverlay(duplicate)
        return duplicate
    }
}
