package com.vibi.shared.platform

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * [MediaMetadataRetriever] 로 영상 한 프레임을 추출 → JPEG q80 으로 cacheDir/thumbs/<hash>_<atMs>.jpg 저장.
 *
 * iOS [IosVideoThumbnailExtractor] 등가 — 동일 (uri, atMs) 두 번째 호출은 file existence check 만으로
 * 경로 반환. 반환 경로는 file:// scheme 없는 절대 경로 (Coil `AsyncImage` model 로 직결되는 iOS 계약 일치).
 *
 * setDataSource + getFrameAtTime + JPEG compress 가 동기 디스크 I/O 이므로 [Dispatchers.IO] 로 분리.
 * 실패 시 throw 하지 않고 null 반환 — 호출자가 placeholder 로 graceful 처리.
 */
class AndroidVideoThumbnailExtractor(
    private val context: Context,
) : VideoThumbnailExtractor {

    override suspend fun extractThumbnail(uri: String, atMs: Long): String? = withContext(Dispatchers.IO) {
        val dest = File(context.cacheDir, "thumbs/${uri.hashCode().toUInt()}_$atMs.jpg")
        // cache hit — 이미 추출된 썸네일이면 디코드/인코드 생략.
        if (dest.exists()) return@withContext dest.absolutePath
        dest.parentFile?.mkdirs()

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(uri))

            // MediaMetadataRetriever 의 time 단위는 마이크로초 — atMs(ms) × 1000.
            val timeUs = atMs * 1000L
            val bitmap: Bitmap? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    // getScaledFrameAtTime: 720×720 박스에 맞춰 디코드 단계에서 다운스케일 (메모리 절약).
                    retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        720,
                        720,
                    )
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }

            bitmap ?: return@withContext null

            FileOutputStream(dest).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            dest.absolutePath
        } catch (e: Exception) {
            // 손상/판독불가/권한만료 등 — 부분 기록 파일은 정리하고 null 로 떨어뜨린다.
            runCatching { if (dest.exists()) dest.delete() }
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
