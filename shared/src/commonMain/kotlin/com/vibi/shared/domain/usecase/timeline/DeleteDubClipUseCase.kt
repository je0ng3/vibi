package com.vibi.shared.domain.usecase.timeline

import com.vibi.shared.domain.repository.DubClipRepository
import com.vibi.shared.platform.deleteLocalFile
import com.vibi.shared.platform.fileExists

class DeleteDubClipUseCase(
    private val dubClipRepository: DubClipRepository
) {
    suspend operator fun invoke(clipId: String) {
        val clip = dubClipRepository.getClip(clipId)
        if (clip != null) {
            if (fileExists(clip.audioFilePath)) {
                deleteLocalFile(clip.audioFilePath)
            }
            dubClipRepository.deleteClip(clipId)
        }
    }
}
