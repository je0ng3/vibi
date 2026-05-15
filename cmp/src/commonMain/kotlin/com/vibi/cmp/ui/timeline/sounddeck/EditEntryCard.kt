package com.vibi.cmp.ui.timeline.sounddeck

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibi.cmp.theme.LocalVibiColors

/**
 * Stepper 가 사라진 unified 모드에서 segment edit mode 진입의 유일한 명시 경로.
 *
 * leading 은 16dp Edit 아이콘만 전달 — [IconLabelCard] 가 24dp 원형 컨테이너로 wrap.
 */
@Composable
fun EditEntryCard(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalVibiColors.current
    IconLabelCard(
        label = "영상 다듬기",
        description = "구간 자르기 · 속도 · 복제 · 삭제",
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            tint = tokens.onBackgroundPrimary,
            modifier = Modifier.size(16.dp),
        )
    }
}
