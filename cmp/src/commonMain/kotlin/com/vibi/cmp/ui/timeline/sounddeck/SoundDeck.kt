package com.vibi.cmp.ui.timeline.sounddeck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibi.cmp.platform.rememberAudioPreviewer
import com.vibi.cmp.theme.LocalVibiColors

/**
 * 분리된 stem + BGM 들을 세로 카드 스택으로 보여주는 컨테이너.
 *
 * 책임:
 *  - 카드 정렬·라벨링은 [SoundCardModel] (buildSoundDeck) 에 위임
 *  - 카드 한 장의 인터랙션은 [SoundCard] 에 위임
 *  - 본 컴포저블은 "현재 어떤 카드를 미리듣고 있는가" 로컬 상태와 분기 호출만
 *
 * 진행 중 잡(분리/생성) 일 때 [disabled=true] — 카드 alpha 낮추고 클릭 ignore.
 */
@Composable
fun SoundDeck(
    cards: List<SoundCardModel>,
    disabled: Boolean,
    onToggleStem: (directiveId: String, stemId: String, selected: Boolean) -> Unit,
    onUpdateStemVolume: (directiveId: String, stemId: String, volume: Float) -> Unit,
    onUpdateBgmVolume: (clipId: String, volume: Float) -> Unit,
    onDeleteBgm: (clipId: String) -> Unit,
    onAddSeparation: (() -> Unit)?,
    onAddBgm: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalVibiColors.current
    val previewer = rememberAudioPreviewer()
    var previewingKey by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            previewer.stop()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "소리",
                style = MaterialTheme.typography.titleSmall,
                color = tokens.onBackgroundPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (cards.isEmpty()) "분리된 소리·삽입한 음원이 여기 카드로 모입니다"
                else "탭으로 끄기 · 길게 눌러 볼륨 조절",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.mutedText,
            )
        }

        cards.forEach { card ->
            key(card.key) {
            SoundCard(
                model = card,
                disabled = disabled,
                isPreviewing = previewingKey == card.key,
                onToggle = {
                    when (val src = card.source) {
                        is SoundCardSource.SeparationStem ->
                            onToggleStem(src.directiveId, src.stemId, !card.selected)
                        is SoundCardSource.Bgm -> {
                            val next = if (card.volume > 0f) 0f else 1f
                            onUpdateBgmVolume(src.clipId, next)
                        }
                    }
                },
                onUpdateVolume = { v ->
                    when (val src = card.source) {
                        is SoundCardSource.SeparationStem ->
                            onUpdateStemVolume(src.directiveId, src.stemId, v)
                        is SoundCardSource.Bgm ->
                            onUpdateBgmVolume(src.clipId, v)
                    }
                },
                onTogglePreview = {
                    val url = card.audioUrl
                    if (url.isNullOrBlank()) return@SoundCard
                    if (previewingKey == card.key) {
                        previewer.stop()
                        previewingKey = null
                    } else {
                        previewer.stop()
                        previewer.play(
                            url = url,
                            volume = card.volume.coerceIn(0f, 1f),
                            onComplete = { previewingKey = null },
                        )
                        previewingKey = card.key
                    }
                },
                onDelete = when (val src = card.source) {
                    is SoundCardSource.Bgm -> ({
                        if (previewingKey == card.key) {
                            previewer.stop()
                            previewingKey = null
                        }
                        onDeleteBgm(src.clipId)
                    })
                    is SoundCardSource.SeparationStem -> null
                },
            )
            }
        }

        if (onAddSeparation != null) {
            AddSourceCard(
                label = "+ 음원 분리",
                description = "원하는 구간을 골라 화자·배경음으로 나눠요",
                enabled = !disabled,
                onClick = onAddSeparation,
            )
        }
        if (onAddBgm != null) {
            AddSourceCard(
                label = "+ 음원 삽입",
                description = "BGM 파일 업로드 또는 즉시 녹음",
                enabled = !disabled,
                onClick = onAddBgm,
            )
        }
    }
}
