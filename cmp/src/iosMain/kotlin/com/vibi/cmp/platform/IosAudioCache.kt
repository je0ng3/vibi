@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.vibi.cmp.platform

import com.vibi.shared.platform.iosCachesDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToFile

/**
 * stem URL 이 path-only (`/api/v2/...`) 로 들어온 경우 plist 의 BFFBaseURL 을 직접 읽어 prepend.
 * KMP `:shared` 의 absUrl 변환이 framework 빌드 캐시 등으로 stale 일 때를 위한 self-contained 안전망.
 *
 * - http(s):// 면 그대로
 * - "/" 시작이면 BFFBaseURL prepend (BFFBaseURL 없으면 입력 그대로)
 * - 그 외 (file path 등) 그대로
 */
internal fun resolveAbsoluteAudioUrl(url: String): String {
    if (url.startsWith("http://") || url.startsWith("https://")) return url
    if (!url.startsWith("/")) return url
    val baseUrl = (NSBundle.mainBundle.objectForInfoDictionaryKey("BFFBaseURL") as? String)
        ?.takeIf { it.isNotEmpty() } ?: return url
    return "${baseUrl.trimEnd('/')}/${url.trimStart('/')}"
}

/**
 * remote URL 의 audio 를 background thread (Dispatchers.Default) 에서 다운로드 후 caches dir 의
 * 임시 파일로 저장하고 그 path 를 반환. 실패 시 null.
 *
 * iOS 가 main thread 동기 NSURLConnection 호출을 silent fail 시키므로 [Dispatchers.Default] 강제.
 *
 * @param url 다운로드 대상 URL (이미 absolute. caller 가 [resolveAbsoluteAudioUrl] 로 보정 후 호출)
 * @param prefix 임시 파일명 prefix ("preview" / "stem" 등 — 로그/회수 디버깅용)
 * @return 저장된 임시 파일 절대 경로. caller 가 [deleteCachedAudio] 로 정리.
 */
internal suspend fun downloadAudioToCache(url: String, prefix: String): String? {
    val nsUrl = NSURL.URLWithString(url) ?: return null
    val data = withContext(Dispatchers.Default) {
        NSData.dataWithContentsOfURL(nsUrl)
    } ?: return null
    val ext = url.substringAfterLast('.', "").substringBefore('?').lowercase()
        .ifEmpty { "audio" }
    val cachesDir = iosCachesDirectory() ?: return null
    val tempPath = "$cachesDir/${prefix}_${NSUUID().UUIDString()}.$ext"
    @Suppress("CAST_NEVER_SUCCEEDS")
    if (!data.writeToFile(tempPath, atomically = true)) return null
    return tempPath
}

internal fun deleteCachedAudio(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, null)
}
