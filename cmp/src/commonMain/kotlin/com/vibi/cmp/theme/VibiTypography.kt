package com.vibi.cmp.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * DESIGN.md typography 토큰 15종. Material3 의 [androidx.compose.material3.Typography] 와
 * 별개 — Material slot(displayLarge/headlineMedium/...) 로는 표현 못 하는 EB Garamond Light(300) +
 * Inter mixed family / mono-time 같은 도메인 전용 스타일을 보존.
 *
 * fontFamily 는 폰트 ttf 가 아직 commonMain/composeResources/font 에 번들 안 돼 system fallback
 * (SF Pro on iOS, Roboto on Android) 으로 렌더링됨. 폰트 도착 시 [DisplayFamily] / [BodyFamily] /
 * [MonoFamily] 세 줄만 교체하면 전 코드에 자동 반영.
 *
 * letterSpacing 단위:
 *  - DESIGN.md 는 px (e.g. -0.8px @ 40px = -0.02em). Compose 의 `.em` 은 fontSize 비례라 모든
 *    크기에서 비율 유지 — px → em 변환 (px / fontSize) 후 .em 로 표현.
 */
@Immutable
data class VibiTypography(
    /** 40/300/-0.02em — 메인 hero (모바일 캡). */
    val displayHero: TextStyle,
    /** 32/300/-0.01em — 서브 hero. */
    val displayXl: TextStyle,
    /** 28/300/-0.01em — 섹션 헤드. */
    val displayLg: TextStyle,
    /** 24/300/-0.01em — CTA section / 그룹 타이틀. */
    val displayMd: TextStyle,
    /** 20/300/0 — bottom-sheet 헤더. */
    val displaySm: TextStyle,
    /** 18/500 — 카드 타이틀 (Inter). */
    val titleLg: TextStyle,
    /** 17/500 — top-bar / list section header. */
    val titleMd: TextStyle,
    /** 15/500/+0.01em — row 라벨 / 강조 캡션. */
    val titleSm: TextStyle,
    /** 16/400/+0.01em — 기본 body. */
    val bodyMd: TextStyle,
    /** 16/500/+0.01em — 강조 body. */
    val bodyStrong: TextStyle,
    /** 14/400/+0.01em — 보조 body / chip. */
    val bodySm: TextStyle,
    /** 13/400 — 사진 캡션 / 시간. */
    val caption: TextStyle,
    /** 11/600/+0.08em UPPERCASE — 섹션 라벨 / badge. */
    val captionUppercase: TextStyle,
    /** 16/500 — CTA pill. */
    val button: TextStyle,
    /** 11/500/+0.01em — 탭바 라벨. */
    val tabLabel: TextStyle,
    /** 13/500 mono — 타임코드 / 재생 위치. */
    val monoTime: TextStyle,
)

// 폰트 번들 도착 시 이 세 줄만 갈아끼움. 나머지 토큰은 무변경.
private val DisplayFamily: FontFamily = FontFamily.Serif    // → EB Garamond Light
private val BodyFamily: FontFamily = FontFamily.SansSerif   // → Inter
private val MonoFamily: FontFamily = FontFamily.Monospace   // → JetBrains Mono / SF Mono

val DefaultVibiTypography = VibiTypography(
    displayHero = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Light,
        fontSize = 40.sp,
        lineHeight = 43.sp,        // 1.08
        letterSpacing = (-0.02).em,
    ),
    displayXl = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Light,
        fontSize = 32.sp,
        lineHeight = 36.sp,        // 1.13
        letterSpacing = (-0.01).em,
    ),
    displayLg = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Light,
        fontSize = 28.sp,
        lineHeight = 33.sp,        // 1.18
        letterSpacing = (-0.01).em,
    ),
    displayMd = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Light,
        fontSize = 24.sp,
        lineHeight = 29.sp,        // 1.2
        letterSpacing = (-0.01).em,
    ),
    displaySm = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Light,
        fontSize = 20.sp,
        lineHeight = 25.sp,        // 1.25
    ),
    titleLg = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 25.sp,        // 1.4
    ),
    titleMd = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,        // 1.4
    ),
    titleSm = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,        // 1.4
        letterSpacing = 0.01.em,
    ),
    bodyMd = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,        // 1.5
        letterSpacing = 0.01.em,
    ),
    bodyStrong = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.em,
    ),
    bodySm = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,        // 1.5
        letterSpacing = 0.01.em,
    ),
    caption = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,        // 1.4
    ),
    // Compose 는 textTransform 미지원 → 호출처에서 .uppercase() 적용.
    captionUppercase = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,        // 1.4
        letterSpacing = 0.08.em,
    ),
    button = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 16.sp,        // 1.0
    ),
    tabLabel = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 13.sp,        // 1.2
        letterSpacing = 0.01.em,
    ),
    monoTime = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 13.sp,        // 1.0
    ),
)

val LocalVibiTypography = staticCompositionLocalOf { DefaultVibiTypography }
