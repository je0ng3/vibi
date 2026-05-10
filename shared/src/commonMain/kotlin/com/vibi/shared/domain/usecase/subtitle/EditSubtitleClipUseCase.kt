package com.vibi.shared.domain.usecase.subtitle

import com.vibi.shared.domain.model.SubtitleClip
import com.vibi.shared.domain.model.SubtitlePosition
import com.vibi.shared.domain.repository.SubtitleClipRepository

class EditSubtitleClipUseCase constructor(
    private val subtitleClipRepository: SubtitleClipRepository
) {
    suspend operator fun invoke(
        clip: SubtitleClip,
        text: String? = null,
        startMs: Long? = null,
        endMs: Long? = null,
        position: SubtitlePosition? = null,
        fontFamily: String? = null,
        fontSizeSp: Float? = null,
        colorHex: String? = null,
        backgroundColorHex: String? = null,
    ): SubtitleClip {
        val updated = clip.copy(
            text = text ?: clip.text,
            startMs = startMs ?: clip.startMs,
            endMs = endMs ?: clip.endMs,
            position = position ?: clip.position,
            fontFamily = fontFamily ?: clip.fontFamily,
            fontSizeSp = fontSizeSp ?: clip.fontSizeSp,
            colorHex = colorHex ?: clip.colorHex,
            backgroundColorHex = backgroundColorHex ?: clip.backgroundColorHex,
        )
        require(updated.endMs > updated.startMs) { "endMs must be greater than startMs" }
        subtitleClipRepository.updateClip(updated)
        return updated
    }
}
