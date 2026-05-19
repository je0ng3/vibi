package com.vibi.cmp.theme

import androidx.compose.ui.graphics.Color

/**
 * 화자 stem (speaker_1, speaker_2, ...) 별 시각 색. 음원분리 UI 의 SoundDeck 카드 chip + 타임라인
 * 파형 highlight 가 같은 매핑을 공유해, 같은 화자가 두 군데서 같은 색으로 인지된다.
 *
 * palette 는 [VibiColors.gradient*] 5종에서 hue 가 잘 떨어진 순서로 선택. 6+ 화자는 wrap-around.
 */
object SpeakerPalette {
    fun colorFor(speakerIndex: Int?, tokens: VibiColors): Color {
        val palette = listOf(
            tokens.gradientSky,
            tokens.gradientPeach,
            tokens.gradientMint,
            tokens.gradientRose,
            tokens.gradientLavender,
        )
        val i = ((speakerIndex ?: 1) - 1).coerceAtLeast(0) % palette.size
        return palette[i]
    }
}
