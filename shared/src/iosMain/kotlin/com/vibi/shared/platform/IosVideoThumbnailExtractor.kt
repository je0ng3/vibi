@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.vibi.shared.platform

import platform.AVFoundation.AVAsset
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVURLAssetPreferPreciseDurationAndTimingKey
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToFile
import kotlinx.cinterop.autoreleasepool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGImageRelease
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

/**
 * AVAssetImageGenerator 로 0초 시점 CGImage 추출 → UIImage → JPEG (NSData) → cacheDir/thumbs/<hash>.jpg.
 * 동일 uri 의 두 번째 호출은 file existence check 만으로 path 반환.
 *
 * 주의: K/N 의 NSURL 절대 경로 처리는 known-bug — `URLWithString(absolutePath)` 가 nil 안 던짐.
 * IosVideoMetadataExtractor 와 동일 패턴으로 분기.
 */
class IosVideoThumbnailExtractor : VideoThumbnailExtractor {

    override suspend fun extractThumbnail(uri: String, atMs: Long): String? = withContext(Dispatchers.Default) {
        // copyCGImageAtTime + JPEG encode + writeToFile 가 100-300ms 동기 디스크 호출.
        // InputViewModel 이 모든 draft 의 썸네일을 awaitAll 로 묶어 추출하기 때문에 caller dispatcher
        // (Main) 에서 동작하면 N drafts × 100-300ms = 첫 진입 시 UI freeze.
        //
        // autoreleasepool: UIImage / UIImageJPEGRepresentation(NSData) 는 autorelease 객체인데
        // Dispatchers.Default 워커 스레드엔 pool 을 비울 run loop 가 없어 awaitAll fan-out 동안
        // batch 전체가 누적된다. pool 로 감싸 호출 종료 즉시 회수. (autoreleasepool 은 inline 이라
        // return@withContext 비지역 반환 통과 OK.)
        autoreleasepool {
            val cacheDir = "${cacheDirectory()}/thumbs"
            ensureDir(cacheDir)
            val cachePath = "$cacheDir/${uri.hashCode().toUInt()}_${atMs}.jpg"
            if (NSFileManager.defaultManager.fileExistsAtPath(cachePath)) return@withContext cachePath

            val url = resolveStoredUriToFileUrl(uri) ?: return@withContext null
            val asset: AVAsset = AVURLAsset(
                uRL = url,
                options = mapOf(AVURLAssetPreferPreciseDurationAndTimingKey to true)
            )
            val generator = AVAssetImageGenerator(asset).apply {
                appliesPreferredTrackTransform = true
                maximumSize = CGSizeMake(720.0, 720.0)
            }

            val time = CMTimeMake(value = atMs * 600L / 1000L, timescale = 600)
            val cgImage = runCatching {
                generator.copyCGImageAtTime(requestedTime = time, actualTime = null, error = null)
            }.getOrNull() ?: return@withContext null

            // copyCGImageAtTime 은 Create Rule — +1 소유권 CGImageRef 반환. K/N 은 CF 객체를 ARC
            // 관리하지 않으므로 UIImage 가 retain 한 뒤에도 우리 +1 은 직접 release 해야 누수 안 남.
            // JPEG 인코딩 실패해도 release 되도록 finally.
            val data = try {
                val uiImage = UIImage.imageWithCGImage(cgImage)
                UIImageJPEGRepresentation(uiImage, 0.8)
            } finally {
                CGImageRelease(cgImage)
            } ?: return@withContext null

            val ok = data.writeToFile(cachePath, atomically = true)
            if (ok) cachePath else null
        }
    }

    private fun cacheDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        return requireNotNull(paths.firstOrNull() as? String) { "Could not resolve iOS cache dir." }
    }

    private fun ensureDir(path: String) {
        val fm = NSFileManager.defaultManager
        if (!fm.fileExistsAtPath(path)) {
            fm.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
        }
    }
}
