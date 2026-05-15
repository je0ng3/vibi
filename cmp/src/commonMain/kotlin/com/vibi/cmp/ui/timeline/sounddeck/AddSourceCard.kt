package com.vibi.cmp.ui.timeline.sounddeck

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = tokens.onBackgroundPrimary,
            modifier = Modifier.size(16.dp),
        )
    }
}
