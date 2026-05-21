package com.vibi.cmp.theme

import androidx.compose.ui.unit.dp

/**
 * DESIGN.md spacing 토큰 — 4px base unit, 9 단계.
 *
 * 새 코드는 raw `12.dp` 대신 [VibiSpacing.sm] 같은 토큰을 사용.
 * 디자인 스케일 외 값(6dp, 14dp, 22dp 등)이 도처에 박히는 걸 방지하기 위함.
 */
object VibiSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val base = 16.dp
    val md = 20.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 40.dp

    // 터치 타겟 전용 — iOS HIG 44pt / Material 3 48dp 기준. 일반 spacing 토큰(xxl=40dp)이
    // 44 미만이라 무심코 .size(VibiSpacing.xxl) 한 곳들이 모두 기준 미달이 되던 문제 회피.
    val touchMin = 44.dp
}
