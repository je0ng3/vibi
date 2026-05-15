package com.vibi.cmp.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * DESIGN.md rounded 토큰 — 모바일 튜닝 스케일.
 *
 * 새 코드는 raw `12.dp` (스케일에 없음) 대신 [VibiRadius.lg] 같은 토큰 사용.
 *  - none/xs/sm/md/lg/xl/xxl 은 카드/입력 등 일반 표면용
 *  - pill / full 은 9999.dp — CTA / chip / avatar / icon-button
 *
 * RoundedCornerShape 도 같이 노출 — `RoundedCornerShape(VibiRadius.lg)` 대신
 * [VibiShape.lg] 한 줄로 끝.
 */
object VibiRadius {
    val none = 0.dp
    /** timeline segment block. */
    val xs = 4.dp
    /** subtitle overlay. */
    val sm = 6.dp
    /** text-input. */
    val md = 10.dp
    /** panel-card. */
    val lg = 14.dp
    /** feature-card. */
    val xl = 18.dp
    /** bottom-sheet top / gradient-orb-card. */
    val xxl = 24.dp
    /** CTA button / badge / chip. */
    val pill = 9999.dp
    /** voice icon / avatar / icon-button. */
    val full = 9999.dp
}

object VibiShape {
    val xs = RoundedCornerShape(VibiRadius.xs)
    val sm = RoundedCornerShape(VibiRadius.sm)
    val md = RoundedCornerShape(VibiRadius.md)
    val lg = RoundedCornerShape(VibiRadius.lg)
    val xl = RoundedCornerShape(VibiRadius.xl)
    val xxl = RoundedCornerShape(VibiRadius.xxl)
    val pill = RoundedCornerShape(VibiRadius.pill)
}
