@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.dubcast.shared.platform

import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * iOS 저장 sourceUri ↔ NSURL/path 변환 유틸.
 *
 * **배경**: iOS 시뮬레이터/실기기에서 app 재설치/build version 변경 시 container UUID 가 바뀜.
 * 그러면 DB 에 저장된 절대경로 (`/Users/.../Application/<OLD_UUID>/Documents/picker_media/foo.mov`)
 * 는 invalid → 영상 재생 시 흰 화면, NSData.dataWithContentsOfURL nil, AVURLAsset.tracks empty.
 *
 * **해결 전략**:
 *  1) 신규 picker 결과는 *상대경로* (`picker_media/foo.mov`) 로 저장 — Documents 기준으로 resolve.
 *  2) DB 의 옛 절대경로도 best-effort remap — `/Documents/` 마커 이후 부분을 추출해 현재 Documents
 *     에 합쳐 NSURL 생성. 같은 simulator 안의 build UUID 변경 케이스 복구 가능.
 *  3) `file://` scheme prefix 도 수용 — URLWithString fallback (K/N NSURL 절대경로 bug 회피).
 *
 * resolver 헬퍼는 iosMain 어느 모듈에서든 호출 가능 — `:cmp/iosMain` 도 `:shared` 의존성을 통해
 * 같은 함수를 쓸 수 있게 `internal` 이 아닌 `public` 함수로 노출.
 */

/** 현재 앱 컨테이너의 NSDocumentDirectory 절대경로. trailing slash 없음. */
fun iosDocumentsDirectory(): String? {
    val paths = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, true
    )
    return paths.firstOrNull() as? String
}

/** 현재 앱 컨테이너의 NSCachesDirectory 절대경로. trailing slash 없음. */
fun iosCachesDirectory(): String? {
    val paths = NSSearchPathForDirectoriesInDomains(
        NSCachesDirectory, NSUserDomainMask, true
    )
    return paths.firstOrNull() as? String
}

/**
 * 저장된 sourceUri 를 현재 app container 기준 절대 file path 로 변환.
 *
 * 입력 케이스 분기:
 *  - `file://` prefix → `removePrefix("file://")` (절대 또는 상대 path 모두 처리)
 *  - 절대경로 (`/...`) + `/Documents/` 마커 포함 → 마커 이후 부분만 현재 Documents 와 join (UUID 변경 복구).
 *  - 절대경로 (`/...`) + 마커 없음 → 그대로 사용 (시스템 path 등).
 *  - 상대경로 (`picker_media/...`) → 현재 Documents 와 join.
 *
 * 결과 path 는 [NSFileManager.fileExistsAtPath] 로 추가 검증 가능. 본 함수는 단순 변환만.
 */
fun resolveStoredUriToPath(stored: String): String? {
    if (stored.isEmpty()) return null

    // 1) file:// scheme — strip 후 동일 로직 재진입.
    if (stored.startsWith("file://")) {
        return resolveStoredUriToPath(stored.removePrefix("file://"))
    }

    // 2) 절대경로 — `/Documents/` 또는 `/Library/Caches/` 마커 검사.
    if (stored.startsWith("/")) {
        // Documents marker 우선 검사.
        val docsMarker = "/Documents/"
        val docsIdx = stored.indexOf(docsMarker)
        if (docsIdx >= 0) {
            val rel = stored.substring(docsIdx + docsMarker.length)
            val currentDocs = iosDocumentsDirectory()
            if (currentDocs != null) {
                val candidate = "$currentDocs/$rel"
                if (NSFileManager.defaultManager.fileExistsAtPath(candidate)) {
                    return candidate
                }
            }
            // remap 실패 시 원본 path 를 그대로 반환 — UUID 안 바뀐 경우엔 원본이 valid.
            return stored
        }
        // Caches marker — recording 등 NSCachesDirectory 저장물.
        val cachesMarker = "/Library/Caches/"
        val cachesIdx = stored.indexOf(cachesMarker)
        if (cachesIdx >= 0) {
            val rel = stored.substring(cachesIdx + cachesMarker.length)
            val currentCaches = iosCachesDirectory()
            if (currentCaches != null) {
                val candidate = "$currentCaches/$rel"
                if (NSFileManager.defaultManager.fileExistsAtPath(candidate)) {
                    return candidate
                }
            }
            return stored
        }
        return stored
    }

    // 3) 상대경로 — 현재 Documents 와 join.
    val currentDocs = iosDocumentsDirectory() ?: return null
    return "$currentDocs/$stored"
}

/**
 * 저장된 sourceUri 를 NSURL 로 변환. 모든 분기에서 `fileURLWithPath` 사용 — K/N 의
 * `URLWithString(absolutePath)` invalid-URL 버그 회피.
 *
 * 반환 NSURL 은 file:// scheme. 파일 존재 여부 검증 안 함 — 호출자가 별도 체크.
 */
fun resolveStoredUriToFileUrl(stored: String): NSURL? {
    val path = resolveStoredUriToPath(stored) ?: return null
    return NSURL.fileURLWithPath(path)
}

/**
 * 저장된 sourceUri 의 파일이 실제 존재하는지 검사. UI 에서 미리보기 placeholder 분기용.
 */
fun storedUriFileExists(stored: String): Boolean {
    val path = resolveStoredUriToPath(stored) ?: return false
    return NSFileManager.defaultManager.fileExistsAtPath(path)
}
