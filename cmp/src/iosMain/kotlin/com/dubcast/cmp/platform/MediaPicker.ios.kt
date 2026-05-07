package com.dubcast.cmp.platform

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.dubcast.shared.platform.currentTimeMillis
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSItemProvider
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeMovie
import platform.darwin.NSObject

private val SystemBlue = Color(0xFF007AFF)

/**
 * iOS MediaPicker — `PHPickerViewController` (iOS 14+) 통합.
 *
 * **PHPicker 의 임시 file URL 은 picker dismiss 후 만료**되므로 loadFileRepresentation
 * 콜백 안에서 즉시 NSDocumentDirectory 로 복사하고 영구 경로를 [onPicked] 로 전달.
 *
 * **상대경로 반환**: app container UUID 가 재설치/build version 변경 시 바뀌므로 절대경로
 * (`/Users/.../Application/<UUID>/Documents/picker_media/foo.mov`) 를 그대로 저장하면 UUID
 * 변경 후 invalid path 가 됨. `picker_media/<filename>` 같은 Documents-relative path 만 저장하고,
 * 재생/업로드 시점에 `IosFilePathResolver` 가 현재 Documents 기준으로 resolve.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun MediaPicker(
    label: String,
    onPicked: (uri: String) -> Unit
) {
    val launch = rememberMediaPickerLauncher(onPicked)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { launch() }
    ) {
        Text(text = label, style = TextStyle(fontSize = 17.sp, color = SystemBlue))
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun rememberMediaPickerLauncher(
    onPicked: (uri: String) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    val delegateHolder = remember { mutableStateOf<PHPickerViewControllerDelegateProtocol?>(null) }
    return remember(scope, onPicked) {
        {
            presentPhPicker(scope, delegateHolder, onPicked)
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun presentPhPicker(
    scope: CoroutineScope,
    delegateHolder: androidx.compose.runtime.MutableState<PHPickerViewControllerDelegateProtocol?>,
    onPicked: (uri: String) -> Unit,
) {
    val config = PHPickerConfiguration().apply {
        selectionLimit = 1L
        filter = PHPickerFilter.videosFilter
    }
    val picker = PHPickerViewController(configuration = config)

    val pickerDelegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
        override fun picker(
            picker: PHPickerViewController,
            didFinishPicking: List<*>
        ) {
            picker.dismissViewControllerAnimated(flag = true, completion = null)

            val first = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
            val provider: NSItemProvider = first.itemProvider

            println("[Picker] loadFileRepresentation requested")
            provider.loadFileRepresentationForTypeIdentifier(
                typeIdentifier = UTTypeMovie.identifier
            ) { tempUrl, error ->
                println("[Picker] callback tempUrl=$tempUrl error=$error")
                val temp = tempUrl as? NSURL ?: return@loadFileRepresentationForTypeIdentifier
                // PHPicker file URL 은 콜백 종료 후 삭제됨 — 동기 복사 필수.
                val permanentPath = copyToDocuments(temp)
                println("[Picker] copied to=$permanentPath")
                if (permanentPath != null) {
                    scope.launch { onPicked(permanentPath) }
                }
            }
        }
    }
    delegateHolder.value = pickerDelegate
    picker.delegate = pickerDelegate

    topViewController()?.presentViewController(
        viewControllerToPresent = picker,
        animated = true,
        completion = null
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun copyToDocuments(tempUrl: NSURL): String? {
    val docs = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, true
    ).firstOrNull() as? String ?: return null
    val relDir = "picker_media"
    val mediaDir = "$docs/$relDir"
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = mediaDir,
        withIntermediateDirectories = true,
        attributes = null,
        error = null
    )
    val fileName = tempUrl.lastPathComponent
        ?: "video_${currentTimeMillis()}.mov"
    val destPath = "$mediaDir/$fileName"

    NSFileManager.defaultManager.removeItemAtPath(destPath, error = null)

    val destUrl = NSURL.fileURLWithPath(destPath)
    val ok = NSFileManager.defaultManager.copyItemAtURL(
        srcURL = tempUrl,
        toURL = destUrl,
        error = null
    )
    // 상대경로 반환 — Documents 기준. 재설치로 container UUID 가 바뀌어도 resolver 가 현재
    // Documents 와 join 해서 valid path 로 복원.
    return if (ok) "$relDir/$fileName" else null
}

private fun topViewController(): UIViewController? {
    val window = UIApplication.sharedApplication.keyWindow ?: return null
    var top: UIViewController? = window.rootViewController
    while (top?.presentedViewController != null) {
        top = top.presentedViewController
    }
    return top
}
