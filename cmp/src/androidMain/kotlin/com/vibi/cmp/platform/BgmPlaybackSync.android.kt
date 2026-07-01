package com.vibi.cmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import com.vibi.shared.domain.model.BgmClip
import kotlin.math.abs

/**
 * 클립별 ExoPlayer 를 hold 한 후 (id, sourceUri) 기준으로 dedupe / 재생성. video 재생 상태와
 * currentMs 에 sync. drift 0.3s 이상이면 위치 재정렬, 그 이하면 자연 재생에 맡김 (glitch 회피).
 *
 * iOS 의 AVAudioPlayer 기반 [BgmPlaybackSync] 와 동일 동작을 androidx.media3 ExoPlayer 로 구현.
 */
@Composable
actual fun BgmPlaybackSync(
    clips: List<BgmClip>,
    isPlaying: Boolean,
    currentMs: Long,
) {
    // ExoPlayer 는 생성 스레드(=메인) 에서만 접근 가능. LaunchedEffect/DisposableEffect 본문은
    // 모두 Compose 메인 디스패처에서 실행되므로 안전.
    val context = LocalContext.current.applicationContext
    val players = remember { mutableMapOf<String, ExoPlayer>() }
    // id → 현재 player 가 로드한 sourceUri. 같은 id 로 sourceUri 가 바뀌면 (재선택/trim 소스 교체)
    // 옛 player 를 release·교체하기 위한 추적. 존재 여부만 보면 stale 오디오가 계속 재생된다.
    val playerUrls = remember { mutableMapOf<String, String>() }
    // player 당 마지막으로 적용한 speed/volume 캐시 — sync 가 매 tick(≈33ms) PlaybackParameters 를
    // 재할당·volume 재설정하는 것을 피한다. player 재생성 시 함께 제거해 fresh 인스턴스(볼륨 1.0)에
    // 목표값이 반드시 다시 반영되도록 한다.
    val lastSpeed = remember { mutableMapOf<String, Float>() }
    val lastVolume = remember { mutableMapOf<String, Float>() }

    DisposableEffect(Unit) {
        onDispose {
            players.values.forEach { it.release() }
            players.clear()
            playerUrls.clear()
            lastSpeed.clear()
            lastVolume.clear()
        }
    }

    val clipsKey = remember(clips) { clips.map { it.id to it.sourceUri }.toSet() }
    LaunchedEffect(clipsKey) {
        val active = clips.associateBy { it.id }
        // 사라진 clip 정리.
        players.keys.filter { it !in active }.forEach { id ->
            players.remove(id)?.release()
            playerUrls.remove(id)
            lastSpeed.remove(id)
            lastVolume.remove(id)
        }
        // 새 clip 또는 sourceUri 변경 시 player 생성.
        active.values.forEach { clip ->
            val existing = players[clip.id]
            val sameUri = existing != null && playerUrls[clip.id] == clip.sourceUri
            if (!sameUri) {
                // sourceUri 가 바뀐 경우 옛 player 를 먼저 정지·제거 (stale 오디오 + 미해제 방지).
                players.remove(clip.id)?.release()
                playerUrls.remove(clip.id)
                lastSpeed.remove(clip.id)
                lastVolume.remove(clip.id)
                val p = runCatching {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(resolveMediaUri(clip.sourceUri)))
                        playWhenReady = false
                        prepare()
                    }
                }.getOrNull() ?: return@forEach
                players[clip.id] = p
                playerUrls[clip.id] = clip.sourceUri
            }
        }
    }

    // 재생 중 + 매 currentMs 갱신 시 sync. clip 의 글로벌 [start, start+globalDur) 범위 안이면
    // 재생 + 위치 정렬 (drift > 0.3s 시), 범위 밖이면 정지+trim 시작점으로 seek.
    LaunchedEffect(isPlaying, currentMs, clips) {
        clips.forEach { clip ->
            val player = players[clip.id] ?: return@forEach
            val speed = clip.speedScale.coerceIn(0.5f, 2.0f)
            val volume = clip.volumeScale.coerceIn(0f, 1f)
            // trim 적용된 source 길이 (sourceTrimStartMs > 0 또는 sourceTrimEndMs > 0 이면 sub-range).
            // globalDur 는 그 값에 speed 반영.
            val globalDurMs = (clip.effectiveSourceDurationMs / speed)
                .toLong().coerceAtLeast(1L)
            val inRange = currentMs in clip.startMs until (clip.startMs + globalDurMs)
            if (!inRange || !isPlaying) {
                if (player.isPlaying) player.pause()
                if (!inRange) {
                    // 범위 밖 = 다음 진입 시 trim 시작점부터. seek 는 in-range 진입 시 보정.
                    player.seekTo(clip.sourceTrimStartMs.coerceAtLeast(0L))
                }
                return@forEach
            }
            // In range + playing — 위치/볼륨/속도 sync. expectedMs 에 sourceTrimStartMs 오프셋 더함.
            // volume/speed 는 값이 실제로 바뀔 때만 적용 — 매 tick PlaybackParameters 재할당 회피.
            if (lastVolume[clip.id] != volume) {
                player.volume = volume
                lastVolume[clip.id] = volume
            }
            if (lastSpeed[clip.id] != speed) {
                // pitch 보정 (pitch=1) — 속도만 바뀌고 음정은 유지. iOS enableRate 와 동일.
                player.playbackParameters = PlaybackParameters(speed)
                lastSpeed[clip.id] = speed
            }
            val expectedMs = ((currentMs - clip.startMs) * speed + clip.sourceTrimStartMs)
                .toLong().coerceAtLeast(0L)
            if (abs(player.currentPosition - expectedMs) > 300L) {
                player.seekTo(expectedMs)
            }
            if (!player.isPlaying) player.play()
        }
    }
}
