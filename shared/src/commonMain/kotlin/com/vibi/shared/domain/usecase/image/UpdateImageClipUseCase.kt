package com.vibi.shared.domain.usecase.image

import com.vibi.shared.domain.model.ImageClip
import com.vibi.shared.domain.repository.ImageClipRepository

class UpdateImageClipUseCase constructor(
    private val repository: ImageClipRepository
) {
    suspend operator fun invoke(clip: ImageClip) {
        require(clip.endMs > clip.startMs) { "endMs must be greater than startMs" }
        repository.updateClip(clip)
    }
}
