package com.vibi.cmp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vibi.cmp.theme.LocalVibiColors
import com.vibi.cmp.theme.LocalVibiTypography
import com.vibi.cmp.theme.VibiShape

/**
 * 팝업([androidx.compose.material3.AlertDialog])의 공통 액션 버튼 — 앱 전체 다이얼로그가 동일하게
 * "테두리 있는 pill" 버튼을 쓰도록 통일한다.
 *
 * 배경은 채우지 않고(아웃라인) [contentColor] 로 테두리와 텍스트를 함께 칠한다. 확인·파괴 액션은
 * accent(기본)나 error 를, 취소는 [LocalVibiColors] 의 mutedText 를 넘겨 강조 계층을 구분한다.
 * disabled 시엔 테두리도 텍스트와 같은 비율(Material 관례 알파 0.38)로 흐려 톤을 맞춘다.
 */
@Composable
fun VibiDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = LocalVibiColors.current.accent,
    enabled: Boolean = true,
) {
    val typo = LocalVibiTypography.current
    val borderColor = if (enabled) contentColor else contentColor.copy(alpha = 0.38f)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = VibiShape.pill,
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
    ) {
        Text(text, style = typo.bodySm)
    }
}
