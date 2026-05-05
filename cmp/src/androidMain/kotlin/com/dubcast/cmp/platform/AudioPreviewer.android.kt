package com.dubcast.cmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

@Composable
actual fun rememberAudioPreviewer(): AudioPreviewerHandle {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build()
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    return remember(player) {
        object : AudioPreviewerHandle {
            private var pendingComplete: (() -> Unit)? = null
            private val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        pendingComplete?.invoke()
                        pendingComplete = null
                    }
                }
            }

            init {
                player.addListener(listener)
            }

            override fun play(url: String, volume: Float, rate: Float, onComplete: () -> Unit) {
                player.stop()
                pendingComplete = onComplete
                player.volume = volume.coerceIn(0f, 1f)
                player.playbackParameters = PlaybackParameters(rate.coerceIn(0.5f, 2.0f))
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
                player.playWhenReady = true
            }

            override fun stop() {
                pendingComplete = null
                player.stop()
            }
        }
    }
}
