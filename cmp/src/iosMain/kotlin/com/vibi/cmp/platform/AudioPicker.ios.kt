@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.vibi.cmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeAIFF
import platform.UniformTypeIdentifiers.UTTypeAudio
import platform.UniformTypeIdentifiers.UTTypeMP3
import platform.UniformTypeIdentifiers.UTTypeMPEG4Audio
import platform.darwin.NSObject

/**
 * iOS: UIDocumentPickerViewController(forOpeningContentTypes = UTTypeAudio).
 * picker 종료 시 임시 URL 을 NSDocumentDirectory/picked_audio 로 복사 → 영구 path 반환.
 */
@Composable
actual fun rememberAudioPicker(
    onPicked: (uri: String) -> Unit,
): AudioPickerLauncher {
    val delegate = remember { mutableStateOf<UIDocumentPickerDelegateProtocol?>(null) }
    return remember {
        object : AudioPickerLauncher {
            override fun launch() {
                // UTTypeAudio (parent) 만으로는 시뮬/일부 기기에서 매칭 누락 — 자식 UTI 도 명시
                // 추가해 MP3/M4A/AIFF/일반 audio 모두 보이게. asCopy=true → iOS 가 자동으로
                // 앱 sandbox 의 Inbox 로 복사 → security-scoped URL 의식 없이 read 가능.
                val types = listOfNotNull<UTType>(
                    UTTypeAudio, UTTypeMP3, UTTypeMPEG4Audio, UTTypeAIFF,
                )
                val picker = UIDocumentPickerViewController(
                    forOpeningContentTypes = types,
                    asCopy = true,
                )
                val pickerDelegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                    override fun documentPicker(
                        controller: UIDocumentPickerViewController,
                        didPickDocumentsAtURLs: List<*>,
                    ) {
                        controller.dismissViewControllerAnimated(true, null)
                        val first = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
                        // asCopy=true 이면 first 는 이미 Inbox 의 임시 path. 그래도 picked_audio
                        // 로 한번 더 옮겨서 epheremal Inbox 정리 영향 안 받게.
                        val accessing = first.startAccessingSecurityScopedResource()
                        try {
                            val path = copyToDocumentsRelative(first, relDir = "picked_audio", fallbackExt = "m4a")
                            if (path != null) onPicked(path)
                        } finally {
                            if (accessing) first.stopAccessingSecurityScopedResource()
                        }
                    }

                    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                        controller.dismissViewControllerAnimated(true, null)
                    }
                }
                delegate.value = pickerDelegate
                picker.delegate = pickerDelegate
                topViewController()?.presentViewController(picker, true, null)
            }
        }
    }
}
