package com.vibi.cmp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vibi.cmp.theme.LocalVibiColors
import com.vibi.cmp.theme.LocalVibiTypography
import com.vibi.cmp.theme.VibiShape
import com.vibi.cmp.theme.VibiSpacing

/**
 * 앱 전체 팝업의 공통 뼈대. Material3 [androidx.compose.material3.AlertDialog] 를 쓰지 않고
 * [Dialog] + [Surface] 로 직접 레이아웃해, 헤더(타이틀 + 우측 상단 X) / 본문 / 단일 primary 액션의
 * 3단 구성과 앱 토큰(패널 배경·xl 라운드·hairline 테두리)을 강제한다.
 *
 * 설계 의도:
 *  - **타이틀**: [com.vibi.cmp.theme.VibiTypography.displaySm] 로 통일 — 바텀시트 헤더와 같은 토큰이라
 *    편집 톤이 일관되고, Material 기본 `headlineSmall`(시스템 폰트) 로 새던 곳을 차단.
 *  - **취소 = 우측 상단 X**: 하단에 취소 버튼을 두지 않는다. 닫기는 X 또는 바깥 탭.
 *  - **하단 = 단일 primary pill**: 확인/파괴 액션 하나만. 아웃라인 pill 두 개가 경쟁하던 약한 계층 제거.
 *    액션이 없는 안내형 팝업은 [primary] 를 비워 X 만 남긴다.
 *
 * @param title 헤더 텍스트.
 * @param onDismiss X / 바깥 탭 시 호출.
 * @param primary 하단 단일 액션 슬롯. 보통 [VibiPrimaryButton] 을 넘긴다. null 이면 액션 없이 X 만.
 * @param content 본문. [ColumnScope] 로 열려 자유롭게 쌓는다.
 */
@Composable
fun VibiDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    primary: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalVibiColors.current
    val typo = LocalVibiTypography.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier,
            shape = VibiShape.xl,
            color = tokens.panelBg,
            contentColor = tokens.onBackgroundPrimary,
            border = BorderStroke(1.dp, tokens.hairline),
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(VibiSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VibiSpacing.base),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = typo.displaySm,
                        color = tokens.onBackgroundPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(VibiSpacing.lg),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = tokens.mutedText,
                        )
                    }
                }
                content()
                primary?.invoke()
            }
        }
    }
}

/**
 * 팝업 하단 단일 액션 — 채워진 잉크 pill. light 는 잉크(#292524)+흰 글자, dark 는 흰 pill+잉크 글자
 * (VibiTheme 의 accent/onPrimary invert 를 그대로 탄다). [destructive] 면 error 컨테이너로 칠해
 * 파괴 액션임을 색으로 알린다. 폭은 팝업 폭을 꽉 채워 큰 터치 타겟 + 명확한 primary 계층을 준다.
 */
@Composable
fun VibiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val tokens = LocalVibiColors.current
    val typo = LocalVibiTypography.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = VibiSpacing.touchMin),
        shape = VibiShape.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (destructive) MaterialTheme.colorScheme.error else tokens.accent,
            contentColor = if (destructive) MaterialTheme.colorScheme.onError
            else MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = tokens.chipBgDisabled,
            disabledContentColor = tokens.chipContentDisabled,
        ),
    ) {
        Text(text, style = typo.button)
    }
}
