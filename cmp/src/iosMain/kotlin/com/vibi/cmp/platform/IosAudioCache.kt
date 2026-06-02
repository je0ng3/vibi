@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.vibi.cmp.platform

import com.vibi.shared.platform.iosCachesDirectory
import kotlinx.cinterop.autoreleasepool
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
 * - `/api/` 시작이면 BFFBaseURL prepend (BFFBaseURL 없으면 입력 그대로)
 * - 그 외 ("/Users/...", "/var/..." 등 filesystem 절대 path 포함) 그대로
 *
 * 주의: `/`로 시작하는 모든 입력을 prepend 하면 즉석 녹음·picker 결과 (NSCachesDirectory /
 * NSDocumentDirectory 절대 path) 까지 remote URL 로 변환되어 download 시도 후 silent fail —
 * 반드시 `/api/` 같이 BFF API path 만 prepend 해야 한다.
 */
internal fun resolveAbsoluteAudioUrl(url: String): String {
    if (url.startsWith("http://") || url.startsWith("https://")) return url
    if (!url.startsWith("/api/")) return url
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
internal suspend fun downloadAudioToCache(url: String, prefix: String): String? =
    withContext(Dispatchers.Default) {
        val nsUrl = NSURL.URLWithString(url) ?: return@withContext null
        // NSData 가 파일 전체를 메모리에 올린다 (autorelease 객체). Dispatchers.Default 워커엔 pool 을
        // 비울 run loop 가 없어 StemMixer 가 여러 stem 을 연달아 받으면 누적된다. write 까지 pool 로
        // 감싸 함수 반환 즉시 회수. (autoreleasepool 은 inline 이라 return@withContext 통과 OK.)
        autoreleasepool {
            val data = NSData.dataWithContentsOfURL(nsUrl) ?: return@withContext null
            val ext = url.substringAfterLast('.', "").substringBefore('?').lowercase()
                .ifEmpty { "audio" }
            val cachesDir = iosCachesDirectory() ?: return@withContext null
            val tempPath = "$cachesDir/${prefix}_${NSUUID().UUIDString()}.$ext"
            @Suppress("CAST_NEVER_SUCCEEDS")
            if (!data.writeToFile(tempPath, atomically = true)) return@withContext null
            tempPath
        }
    }

internal fun deleteCachedAudio(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, null)
}
