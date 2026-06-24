package com.vibi.shared.data.repository

import com.vibi.shared.domain.usecase.share.GallerySaver
import com.vibi.shared.platform.resolveStoredUriToFileUrl
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAssetResourceCreationOptions
import platform.Photos.PHAssetResourceTypeVideo
import platform.Photos.PHPhotoLibrary
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS GallerySaver — `PHPhotoLibrary.performChanges` 로 mp4 를 사진 라이브러리에 추가.
 *
 * 사진 앱에 보이는 asset 파일명은 소스 파일 basename 을 따른다. 렌더 산출물 path 가
 * `…/export.mp4` 로 고정돼 있어, 구버전(`creationRequestForAssetFromVideoAtFileURL` 만 호출)은
 * 사용자가 지정한 [displayName] 을 버리고 항상 "export" 로 저장됐다. 이를 막기 위해
 * `PHAssetCreationRequest.creationRequestForAsset` + `addResourceWithType` 의
 * `PHAssetResourceCreationOptions.originalFilename` 으로 asset 파일명을 [displayName] 으로 명시 지정.
 *
 * iOS 14+ 의 limited photo access (NSPhotoLibraryAddUsageDescription) 가 Info.plist 에 필요.
 * 권한 요청 자체는 본 클래스가 다루지 않음 — 호출 측에서 사전 권한 처리.
 */
class IosGallerySaver : GallerySaver {

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun saveVideo(sourcePath: String, displayName: String): Result<Unit> = runCatching {
        // resolver: 상대 / 절대 / file:// / 옛 UUID remap.
        val url = resolveStoredUriToFileUrl(sourcePath)
            ?: error("Cannot resolve path to save: $sourcePath")

        val assetFileName = sanitizeFileName(displayName)

        suspendCancellableCoroutine { cont ->
            PHPhotoLibrary.sharedPhotoLibrary().performChanges(
                changeBlock = {
                    val request = PHAssetCreationRequest.creationRequestForAsset()
                    val options = PHAssetResourceCreationOptions().apply {
                        originalFilename = assetFileName
                    }
                    request.addResourceWithType(
                        PHAssetResourceTypeVideo,
                        url,
                        options,
                    )
                },
                completionHandler = { success, error ->
                    if (success) {
                        cont.resume(Unit)
                    } else {
                        cont.resumeWithException(
                            RuntimeException(
                                "PHPhotoLibrary save failed: ${error?.localizedDescription ?: "unknown"}"
                            )
                        )
                    }
                }
            )
        }
    }

    /**
     * 사진 앱 asset 파일명으로 안전하게 정제 — path 구분자/개행/제어문자 제거, `.mp4` 확장자 보장,
     * 비면 기본값. [displayName] 이 사용자 입력(프로젝트 제목)이라 임의 문자가 들어올 수 있음.
     */
    private fun sanitizeFileName(raw: String): String {
        val cleaned = raw
            .map { c -> if (c == '/' || c == '\\' || c.code < 0x20) '_' else c }
            .joinToString("")
            .trim()
            .ifBlank { "vibi_export" }
        return if (cleaned.endsWith(".mp4", ignoreCase = true)) cleaned else "$cleaned.mp4"
    }
}
