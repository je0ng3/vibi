package com.vibi.cmp.ui.timeline.sounddeck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibi.cmp.theme.LocalVibiColors
import com.vibi.cmp.theme.LocalVibiTypography
import com.vibi.cmp.theme.VibiShape
import com.vibi.cmp.theme.VibiSpacing

/**
 * 다듬기(볼륨/속도/secondary/삭제) 액션 패널 — 영상 다듬기 모드(전역) 와 BGM 카드 in-card expansion
 * 양쪽에서 동일 레이아웃으로 재사용. 볼륨/속도는 토글, secondary/삭제는 즉시 액션.
 *
 * - [title] 빈 문자열이면 헤더의 제목 영역 생략.
 * - [secondaryActionLabel] / [onSecondaryAction] — 영상은 "복제", BGM 은 "배경음 제거" 등 컨텍스트별
 *   세 번째 액션을 자유 라벨링. 색·outlined 스타일은 동일.
 * - [onCancel] null 이면 닫기(X) 버튼 생략 — BGM in-card 의 경우 부모 카드 collapse 가 닫기 역할.
 */
@Composable
fun EditActionsPanel(
    title: String,
    volume: Float,
    speed: Float,
    onVolumeChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onApplyVolume: (Float) -> Unit,
    onApplySpeed: (Float) -> Unit,
    secondaryActionLabel: String,
    onSecondaryAction: () -> Unit,
    onDelete: () -> Unit,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalVibiColors.current
    val typo = LocalVibiTypography.current
    // 볼륨/속도 슬라이더는 기본 숨김 — 액션 버튼(볼륨/속도)을 누르면 해당 bar 만 펼침.
    // 한 번에 하나만 펼쳐지도록 enum 상태: null = 아무도 안 열림.
    var expanded by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.panelBg, VibiShape.lg)
            .padding(VibiSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 헤더(닫기/제목)는 액션 버튼 행과 분리 — 4 액션 버튼이 행 폭을 가로질러 양 끝 flush 로
        // 분포되도록 SpaceBetween. 헤더가 같은 Row 에 있으면 weight(1f) 가 빈 공간을 흡수해 액션 4개가
        // 우측에 쏠림. 헤더가 없는 호출 (현행 영상/BGM 양쪽 모두) 에서는 헤더 Row 자체 미렌더.
        if (onCancel != null || title.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VibiSpacing.xs),
            ) {
                if (onCancel != null) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(VibiSpacing.touchMin),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = tokens.onBackgroundPrimary,
                        )
                    }
                }
                if (title.isNotEmpty()) {
                    Text(
                        title,
                        style = typo.titleSm,
                        color = tokens.onBackgroundPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        // 4 버튼 동일 폭(weight 1f) + 작은 gap — SpaceBetween 시 좁은 화면에서 가장 긴 라벨
        // ("배경음 제거") 가 잘려 보이지 않던 케이스 회피. weight 가 행 전체를 4등분해 어떤 라벨이든
        // 고정 슬롯에 들어가고, 행 폭 자체가 양 끝까지 채워져 "양쪽 정렬" 요구도 자연스럽게 만족.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(
                onClick = { expanded = if (expanded == "volume") null else "volume" },
                contentPadding = PaddingValues(horizontal = VibiSpacing.xs, vertical = 0.dp),
                modifier = Modifier.weight(1f).height(VibiSpacing.touchMin),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (expanded == "volume") tokens.accent else tokens.onBackgroundPrimary,
                ),
            ) { Text("볼륨", style = typo.bodySm, maxLines = 1) }
            OutlinedButton(
                onClick = { expanded = if (expanded == "speed") null else "speed" },
                contentPadding = PaddingValues(horizontal = VibiSpacing.xs, vertical = 0.dp),
                modifier = Modifier.weight(1f).height(VibiSpacing.touchMin),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (expanded == "speed") tokens.accent else tokens.onBackgroundPrimary,
                ),
            ) { Text("속도", style = typo.bodySm, maxLines = 1) }
            OutlinedButton(
                onClick = onSecondaryAction,
                contentPadding = PaddingValues(horizontal = VibiSpacing.xs, vertical = 0.dp),
                modifier = Modifier.weight(1f).height(VibiSpacing.touchMin),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.accent),
            ) { Text(secondaryActionLabel, style = typo.bodySm, color = tokens.accent, maxLines = 1) }
            OutlinedButton(
                onClick = onDelete,
                contentPadding = PaddingValues(horizontal = VibiSpacing.xs, vertical = 0.dp),
                modifier = Modifier.weight(1f).height(VibiSpacing.touchMin),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.accent),
            ) { Text("삭제", style = typo.bodySm, color = tokens.accent, maxLines = 1) }
        }

        // 볼륨 — 0..2 (0 = 무음, 1 = 그대로, 2 = 2배). Local state 로 슬라이더 위치 즉시 갱신 +
        // onVolumeChange 로 부모에 live commit. parent prop (volume) 이 바뀌면 (예: 적용 / Apply
        // 외부 경로) sliderVal 도 재seed.
        if (expanded == "volume") {
            var sliderVal by remember(expanded, volume) { mutableStateOf(volume) }
            Column(verticalArrangement = Arrangement.spacedBy(VibiSpacing.xxs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("볼륨 ${(sliderVal * 100).toInt()}%", style = typo.bodySm, color = tokens.mutedText)
                    TextButton(onClick = { onApplyVolume(sliderVal) }) { Text("적용") }
                }
                Slider(
                    value = sliderVal,
                    valueRange = 0f..2f,
                    onValueChange = {
                        sliderVal = it
                        onVolumeChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = mutedSliderColors(tokens.mutedText),
                )
            }
        }

        // 속도 — 0.25..4. BGM 호출 측은 onSpeedChange 가 no-op (commit 비용 큼 — applyBgmRangeSpeed
        // 가 lane re-pack + 다른 BGM 들의 startMs 조정 동반). 그렇다고 onValueChange 를 그대로
        // no-op 으로 두면 controlled Slider 라 value prop 이 안 바뀌어 슬라이더가 시각적으로
        // 움직이지 않음 → 사용자 입장에서 "조절이 안됨". local state 로 시각만 즉시 갱신, parent
        // 의 onSpeedChange 는 그대로 호출해 (live preview 원하는 호출은 그쪽에서 처리), 실제
        // commit 은 "적용" 의 onApplySpeed(sliderVal) 가 담당.
        if (expanded == "speed") {
            var sliderVal by remember(expanded, speed) { mutableStateOf(speed) }
            Column(verticalArrangement = Arrangement.spacedBy(VibiSpacing.xxs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val pct = (sliderVal * 100).toInt()
                    Text("속도 ${pct}%", style = typo.bodySm, color = tokens.mutedText)
                    TextButton(onClick = { onApplySpeed(sliderVal) }) { Text("적용") }
                }
                Slider(
                    value = sliderVal,
                    valueRange = 0.25f..4f,
                    onValueChange = {
                        sliderVal = it
                        onSpeedChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = mutedSliderColors(tokens.mutedText),
                )
            }
        }
    }
}

/**
 * 음원분리 결과 / BGM 편집 슬라이더 색상.
 *
 * - thumb · activeTrack: M3 기본 (= colorScheme.primary = 우리 테마 잉크 near-black). 사용자가
 *   "현재 값" 을 즉각 인지할 수 있도록 유지.
 * - inactiveTrack: M3 기본은 surfaceContainerHighest (라벤더 0xFFE6E0E9, 우리 colorScheme
 *   미override) 라 stem 카드 위에서 "보라" 로 인지됨 → mutedText 옅은 회색으로 교체.
 *
 * [base] 는 inactiveTrack tint 의 기준색 (보통 tokens.mutedText).
 */
@Composable
internal fun mutedSliderColors(base: androidx.compose.ui.graphics.Color): SliderColors =
    SliderDefaults.colors(
        inactiveTrackColor = base.copy(alpha = 0.18f),
        inactiveTickColor = base.copy(alpha = 0.18f),
    )
