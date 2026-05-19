package com.vibi.cmp.theme

import androidx.compose.ui.graphics.Color
import com.vibi.shared.domain.model.Stem
import com.vibi.shared.domain.model.StemKind

/**
 * 음원분리 stem 의 시각 색. SoundDeck 카드 chip + 타임라인 파형 highlight 가 같은 매핑을 공유해
 * 같은 화자가 두 군데서 같은 색으로 인지된다.
 *
 * speaker 는 **고채도** — BGM (muted pastel) 과 채도로 구분되도록 vivid 톤 4색. 5+ 화자는 wrap.
 */
object SpeakerPalette {
    private val palette: List<Color> = listOf(
        Color(0xFF1E88E5),  // speaker 1 — blue
        Color(0xFFE65100),  // speaker 2 — deep orange
        Color(0xFF2E7D32),  // speaker 3 — green
        Color(0xFFC2185B),  // speaker 4 — pink
    )

    @Suppress("UNUSED_PARAMETER")
    fun colorFor(speakerIndex: Int?, tokens: VibiColors): Color {
        val i = ((speakerIndex ?: 1) - 1).coerceAtLeast(0) % palette.size
        return palette[i]
    }

    /**
     * stemId → 시각 색. SPEAKER 는 [colorFor], BACKGROUND 는 mutedText, 그 외는 [fallback].
     * SoundCard chip 과 TimelineWaveform highlight 가 같은 진입점을 쓰도록 통합.
     */
    fun stemColor(stemId: String, tokens: VibiColors, fallback: Color): Color =
        when (Stem.kindFromId(stemId)) {
            StemKind.SPEAKER -> colorFor(Stem.speakerIndexFromId(stemId), tokens)
            StemKind.BACKGROUND -> tokens.mutedText
            else -> fallback
        }
}

/**
 * BGM 클립 (파일 삽입 + 즉시 녹음 통합) 의 시각 색. 삽입 순서 (timeline startMs) 기준 1-based
 * 인덱스로 4색 cycle — 사용자가 어떤 BGM 인지 색만으로 구분 가능. 5개 이상은 wrap.
 */
object BgmPalette {
    private fun palette(tokens: VibiColors): List<Color> = listOf(
        tokens.gradientMint,
        tokens.gradientLavender,
        tokens.gradientRose,
        tokens.gradientPeach,
    )

    fun colorFor(bgmIndex: Int?, tokens: VibiColors): Color {
        val p = palette(tokens)
        val i = ((bgmIndex ?: 1) - 1).coerceAtLeast(0) % p.size
        return p[i]
    }
}
