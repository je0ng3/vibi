package com.vibi.cmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.vibi.shared.platform.resolveStoredUriToFileUrl
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSURL
import platform.darwin.NSObject

/**
 * 미리듣기 player.
 *
 * Local file → AVAudioPlayer (synchronous prepareToPlay, deterministic).
 * Remote URL → AVPlayer (streaming).
 *
 * 자연 종료 감지: AVAudioPlayer 의 delegate (audioPlayerDidFinishPlaying) 가 onComplete 호출.
 * stop() 은 delegate 를 미리 떼서 finish 콜백 발화 막음.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun rememberAudioPreviewer(): AudioPreviewerHandle {
    // 다운로드는 백그라운드 (Dispatchers.IO) 에서 — main thread 동기 호출 시 iOS 가 silent fail.
    // 다운로드 후 AVAudioPlayer init / play 는 main thread.
    val scope = rememberCoroutineScope()
    var delegateRef: NSObject? = null
    var localPlayer: AVAudioPlayer? = null
    // 마지막으로 만든 임시 파일 path (remote download 결과). 새 play 또는 dispose 시 삭제.
    var lastTempPath: String? = null
    fun deleteLastTemp() {
        lastTempPath?.let { deleteCachedAudio(it) }
        lastTempPath = null
    }
    DisposableEffect(Unit) {
        onDispose {
            localPlayer?.delegate = null
            localPlayer?.stop()
            deleteLastTemp()
        }
    }
    return remember(scope) {
        object : AudioPreviewerHandle {
            override fun play(url: String, volume: Float, rate: Float, onComplete: () -> Unit) {
                runCatching {
                    val session = AVAudioSession.sharedInstance()
                    session.setCategory(AVAudioSessionCategoryPlayback, null)
                    session.setActive(true, null)
                }
                val clampedVol = volume.coerceIn(0f, 1f)
                val clampedRate = rate.coerceIn(0.5f, 2.0f)

                val absoluteUrl = resolveAbsoluteAudioUrl(url)
                val isRemote = absoluteUrl.startsWith("http://") || absoluteUrl.startsWith("https://")

                fun startWithFile(fileUrl: NSURL) {
                    val player = runCatching {
                        AVAudioPlayer(contentsOfURL = fileUrl, error = null)
                    }.getOrNull() ?: run {
                        println("[AudioPreviewer] AVAudioPlayer init failed: $absoluteUrl"); return
                    }
                    localPlayer?.let {
                        it.delegate = null
                        it.stop()
                    }
                    player.enableRate = true
                    player.volume = clampedVol
                    player.rate = clampedRate
                    val delegate = object : NSObject(), AVAudioPlayerDelegateProtocol {
                        override fun audioPlayerDidFinishPlaying(
                            player: AVAudioPlayer,
                            successfully: Boolean,
                        ) {
                            onComplete()
                        }
                    }
                    delegateRef = delegate
                    player.delegate = delegate
                    player.prepareToPlay()
                    player.play()
                    localPlayer = player
                }

                if (!isRemote) {
                    val fileUrl = resolveStoredUriToFileUrl(absoluteUrl) ?: run {
                        println("[AudioPreviewer] cannot resolve local: $absoluteUrl"); return
                    }
                    startWithFile(fileUrl)
                    return
                }

                scope.launch {
                    val tempPath = downloadAudioToCache(absoluteUrl, prefix = "preview") ?: run {
                        println("[AudioPreviewer] download failed: $absoluteUrl"); return@launch
                    }
                    // 새 임시 파일로 교체 — 이전 파일 삭제.
                    deleteLastTemp()
                    lastTempPath = tempPath
                    startWithFile(NSURL.fileURLWithPath(tempPath))
                }
            }

            override fun stop() {
                localPlayer?.let {
                    it.delegate = null
                    it.stop()
                }
                localPlayer = null
                delegateRef = null
                deleteLastTemp()
            }
        }
    }
}
