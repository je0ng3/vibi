package com.vibi.cmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibi.cmp.theme.LocalVibiColors
import com.vibi.cmp.theme.VibiShape
import com.vibi.cmp.theme.VibiSpacing

/**
 * DESIGN.md `panel-card` — `canvas-soft` bg, `hairline` 1dp border, `rounded.lg` (14), `padding 16`.
 *
 * 카드 안의 sub-panel — 더 약한 계층. 화면 캔버스(`backgroundPrimary`) 위에 살짝 떠 있는 느낌.
 * SoundDeck 의 IconLabelCard / SoundCard 처럼 deck 안에 여러 장 쌓이는 카드는 이걸 사용.
 */
@Composable
fun VibiPanelCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val tokens = LocalVibiColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.panelBgSoft, VibiShape.lg)
            .border(width = 1.dp, color = tokens.hairline, shape = VibiShape.lg)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled) { onClick() } else Modifier)
            .padding(VibiSpacing.base),
    ) {
        content()
    }
}

/**
 * 유저 메뉴/시트에서 쓰는 chip row 컨테이너 — `chipBg` 채움 + 14dp 라운드(테두리 없음). 크레딧 구매·
 * 광고·계정 연결·연결된 identity row 가 모두 같은 chrome 을 공유하도록 컨테이너만 뽑아낸다([content]
 * 는 [RowScope] 로 열려 leading/title/trailing 을 자유롭게 배치). [onClick] 이 null 이면 비클릭 컨테이너.
 *
 * @param verticalPadding row 세로 패딩 — 밀도가 다른 목록 항목(예: identity row 12dp)을 위해 조정 가능.
 */
@Composable
fun VibiChipRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    verticalPadding: Dp = 14.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalVibiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.chipBg)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled) { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = verticalPadding),
        content = content,
    )
}
