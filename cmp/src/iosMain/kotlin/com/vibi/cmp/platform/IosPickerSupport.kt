@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.vibi.cmp.platform

import com.vibi.shared.platform.currentTimeMillis
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

/**
 * 현재 화면 최상단 UIViewController. picker / alert presentation source.
 * keyWindow 의 rootViewController 부터 presentedViewController 체인을 끝까지 타고 내려간다.
 */
internal fun topViewController(): UIViewController? {
    val window = UIApplication.sharedApplication.keyWindow ?: return null
    var top: UIViewController? = window.rootViewController
    while (top?.presentedViewController != null) {
        top = top.presentedViewController
    }
    return top
}

/**
 * picker 가 넘긴 임시 NSURL 파일을 Documents/[relDir] 로 복사하고 Documents-relative 경로를 반환.
 *
 * 절대 경로 (`/Users/.../Application/<UUID>/Documents/...`) 가 아닌 상대 경로 (`relDir/filename`)
 * 를 반환하는 이유: app container UUID 는 재설치/build version 변경 시 바뀌어 invalid path 가
 * 됨. resolver 가 재생/업로드 시점에 현재 Documents 와 join.
 *
 * @param srcUrl picker 가 넘긴 임시 URL (loadFileRepresentation/documentPicker 콜백 인자)
 * @param relDir Documents 아래 서브디렉터리 ("picker_media" / "picked_audio" 등)
 * @param fallbackExt srcUrl.lastPathComponent 가 null 일 때 생성할 파일명의 확장자 ("mov"/"m4a")
 */
internal fun copyToDocumentsRelative(
    srcUrl: NSURL,
    relDir: String,
    fallbackExt: String,
): String? {
    val docs = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, true
    ).firstOrNull() as? String ?: return null
    val dir = "$docs/$relDir"
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = dir,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    val name = srcUrl.lastPathComponent
        ?: "media_${currentTimeMillis()}.$fallbackExt"
    val destPath = "$dir/$name"

    NSFileManager.defaultManager.removeItemAtPath(destPath, error = null)

    val ok = NSFileManager.defaultManager.copyItemAtURL(
        srcURL = srcUrl,
        toURL = NSURL.fileURLWithPath(destPath),
        error = null,
    )
    return if (ok) "$relDir/$name" else null
}
