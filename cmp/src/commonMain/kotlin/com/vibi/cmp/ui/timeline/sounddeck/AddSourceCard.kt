package com.vibi.cmp.ui.timeline.sounddeck

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.vibi.cmp.theme.LocalVibiColors

@Composable
fun AddSourceCard(
    label: String,
    description: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalVibiColors.current
    IconLabelCard(
        label = label,
        description = description,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    ) {
        Text("+", color = tokens.accent, fontSize = 22.sp)
    }
}
