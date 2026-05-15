package com.vibi.cmp.ui.timeline.sounddeck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibi.cmp.theme.LocalVibiColors

/**
 * SoundDeck 의 진입 카드 — leading 시각 단서 + 라벨 + 부연 설명 + 탭 액션 한 묶음.
 * "+ 음원 분리" 같은 placeholder 와 "영상 다듬기" 같은 명시 액션을 같은 형태로 노출해
 * 사용자가 화면 어디서든 일관된 카드 metaphor 로 다음 행동을 인지하게 함.
 */
@Composable
fun IconLabelCard(
    label: String,
    description: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
) {
    val tokens = LocalVibiColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.backgroundPrimary, RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = tokens.chipBg,
                shape = RoundedCornerShape(12.dp),
            )
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leading()
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.onBackgroundPrimary,
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.mutedText,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}
