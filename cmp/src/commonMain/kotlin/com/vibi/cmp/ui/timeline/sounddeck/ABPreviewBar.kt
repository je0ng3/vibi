package com.vibi.cmp.ui.timeline.sounddeck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibi.cmp.theme.LocalVibiColors
import com.vibi.shared.ui.timeline.PreviewMode

/**
 * 하단 고정 A/B 미리듣기 바 — "원본" / "내 믹스" 두 segment 토글.
 *
 * "결과 예측 어려움" 페르소나 고통의 직접 해결책. directive 가 하나도 없으면 의미 없으므로 상위에서
 * hidden. 토글은 mixer 의 stem 볼륨 + video segment volume 둘 다 영향 — 상위 TimelineScreen 의
 * stemSyncKey LaunchedEffect 가 previewMode 를 보고 일괄 처리.
 */
@Composable
fun ABPreviewBar(
    mode: PreviewMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalVibiColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(tokens.panelBg)
            .border(
                width = 1.dp,
                color = tokens.chipBg,
                shape = RoundedCornerShape(24.dp),
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SegmentChip(
            label = "원본",
            selected = mode == PreviewMode.ORIGINAL,
            onClick = { if (mode != PreviewMode.ORIGINAL) onToggle() },
            modifier = Modifier.weight(1f),
        )
        SegmentChip(
            label = "내 믹스",
            selected = mode == PreviewMode.MIX,
            onClick = { if (mode != PreviewMode.MIX) onToggle() },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalVibiColors.current
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) tokens.accent else tokens.panelBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) tokens.backgroundPrimary else tokens.onBackgroundPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 14.sp,
        )
    }
}
