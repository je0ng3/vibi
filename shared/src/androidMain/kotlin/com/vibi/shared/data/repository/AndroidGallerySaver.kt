package com.vibi.shared.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.vibi.shared.domain.usecase.share.GallerySaver
import com.vibi.shared.platform.openInputFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android GallerySaver — mp4 를 사진/동영상 갤러리(Movies/vibi)에 추가.
 *
 * iOS [IosGallerySaver] 와 동일 계약: 소스 경로의 mp4 를 갤러리에 복사하고, 사용자 지정
 * [displayName] 을 파일명으로 사용한다. iOS 의 `originalFilename` 지정과 대응되게 Android 에서는
 * MediaStore `DISPLAY_NAME` 으로 파일명을 명시한다.
 *
 * - API 29+ : Scoped storage. `MediaStore` insert → `IS_PENDING` 토글 → stream copy.
 *   `WRITE_EXTERNAL_STORAGE` 권한 불필요 (`RELATIVE_PATH` = Movies/vibi).
 * - API 28- : 레거시 `WRITE_EXTERNAL_STORAGE` 권한으로 공용 Movies/vibi 에 직접 파일 작성 후
 *   `MediaScannerConnection` 으로 미디어 색인.
 *
 * 권한 요청 자체는 본 클래스가 다루지 않음 — 호출 측에서 사전 권한 처리.
 */
class AndroidGallerySaver(private val context: Context) : GallerySaver {

    override suspend fun saveVideo(sourcePath: String, displayName: String): Result<Unit> = runCatching {
        val fileName = sanitizeFileName(displayName)
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(sourcePath, fileName)
            } else {
                saveViaFile(sourcePath, fileName)
            }
        }
    }

    /**
     * API 29+ : MediaStore 에 pending row 를 만들고 스트림 복사 후 pending 을 해제한다.
     * 실패 시 부분 생성된 row 를 정리한다.
     */
    private fun saveViaMediaStore(sourcePath: String, fileName: String) {
        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val pending = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, MIME_VIDEO_MP4)
            put(MediaStore.Video.Media.RELATIVE_PATH, RELATIVE_DIR)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val itemUri = resolver.insert(collection, pending)
            ?: error("MediaStore insert returned null for $fileName")

        try {
            resolver.openOutputStream(itemUri).use { out ->
                requireNotNull(out) { "Cannot open output stream for $itemUri" }
                openInputFor(sourcePath).use { input ->
                    input.copyTo(out)
                }
            }

            val done = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(itemUri, done, null, null)
        } catch (t: Throwable) {
            runCatching { resolver.delete(itemUri, null, null) }
            throw t
        }
    }

    /**
     * API 28- : 공용 Movies/vibi 디렉터리에 직접 파일을 쓰고 MediaScanner 로 색인한다.
     */
    private fun saveViaFile(sourcePath: String, fileName: String) {
        @Suppress("DEPRECATION")
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val targetDir = File(moviesDir, GALLERY_FOLDER).apply {
            if (!exists()) mkdirs()
        }
        val target = File(targetDir, fileName)

        target.outputStream().use { out ->
            openInputFor(sourcePath).use { input ->
                input.copyTo(out)
            }
        }

        MediaScannerConnection.scanFile(
            context,
            arrayOf(target.absolutePath),
            arrayOf(MIME_VIDEO_MP4),
            null,
        )
    }

    /**
     * 갤러리 파일명으로 안전하게 정제 — path 구분자/제어문자 제거, `.mp4` 확장자 보장, 비면 기본값.
     * iOS [IosGallerySaver] 의 sanitizeFileName 과 동작 일치.
     */
    private fun sanitizeFileName(raw: String): String {
        val cleaned = raw
            .map { c -> if (c == '/' || c == '\\' || c.code < 0x20) '_' else c }
            .joinToString("")
            .trim()
            .ifBlank { "vibi_export" }
        return if (cleaned.endsWith(".mp4", ignoreCase = true)) cleaned else "$cleaned.mp4"
    }

    private companion object {
        const val MIME_VIDEO_MP4 = "video/mp4"
        const val GALLERY_FOLDER = "vibi"
        val RELATIVE_DIR = "${Environment.DIRECTORY_MOVIES}/$GALLERY_FOLDER"
    }
}
