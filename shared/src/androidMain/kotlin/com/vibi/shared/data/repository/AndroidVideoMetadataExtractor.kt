package com.vibi.shared.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.vibi.shared.domain.model.VideoInfo
import com.vibi.shared.domain.usecase.input.VideoMetadataExtractor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * content:// (PickVisualMedia) URI 에서 [MediaMetadataRetriever] 로 영상 메타데이터 추출.
 *
 * iOS [IosVideoMetadataExtractor] 등가 — 검증(길이>300s, 최장변>1920)과 분리 확인 팝업의
 * coarse 게이트에만 쓰여 frame-accurate 가 불필요하므로 container 메타만 읽는다.
 *
 * **실패 시 throw 하지 않고 null 반환**: InputViewModel.onVideoPicked 가 null → METADATA_UNREADABLE
 * 로 graceful 매핑한다. (이전 v1 stub 은 error(...) 를 던져 viewModelScope 코루틴을 죽이고 앱을
 * 크래시시켰다 — "Analyzing…" 에서 멈추는 원인.)
 */
class AndroidVideoMetadataExtractor(
    private val context: Context,
) : VideoMetadataExtractor {

    override suspend fun extract(uri: String): VideoInfo? = withContext(Dispatchers.IO) {
        // MediaMetadataRetriever 는 setDataSource/extract 가 동기 디스크 I/O — IO dispatcher 로 분리.
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(uri))

            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: return@withContext null

            // iOS [IosVideoMetadataExtractor] 와 byte-exact 파리티: iOS 는 videoTrack.naturalSize 를
            // preferredTransform 적용 없이 그대로 보고한다(= raw 인코딩 픽셀, 회전 스왑 없음).
            // 따라서 여기서도 METADATA_KEY_VIDEO_ROTATION 90/270 가로·세로 스왑을 적용하지 않고
            // METADATA_KEY_VIDEO_WIDTH/HEIGHT 원본을 그대로 보고해 iOS naturalSize 와 동일하게 맞춘다.
            val width = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val height = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0

            if (durationMs <= 0L || width <= 0 || height <= 0) return@withContext null

            VideoInfo(
                uri = uri,
                // file:// URI 는 %-인코딩돼 있으므로 디코드해 사람이 읽는 파일명(=프로젝트 타이틀)으로.
                fileName = Uri.decode(uri.substringAfterLast('/')).ifBlank { "video.mp4" },
                mimeType = "video/mp4",
                durationMs = durationMs,
                width = width,
                height = height,
                // iOS 는 NSFileManager 로 실제 파일 크기를 읽는다 — file:// 경로면 File.length() 로 동일하게.
                // content:// 등 실패/비파일이면 0 폴백(iOS 도 실패 시 0).
                sizeBytes = fileSizeOrZero(uri),
            )
        } catch (e: Exception) {
            // 손상/판독불가/권한만료 등 — null 로 떨어뜨려 UI 가 METADATA_UNREADABLE 안내.
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * file:// URI 면 실제 디스크 파일 크기(iOS NSFileManager 등가), 그 외(content:// 등)·실패면 0.
     * 크기는 검증에 불필요하므로 0 폴백이 안전하다.
     */
    private fun fileSizeOrZero(uri: String): Long = runCatching {
        val parsed = Uri.parse(uri)
        if (parsed.scheme == "file") {
            parsed.path?.let { File(it).length() } ?: 0L
        } else {
            0L
        }
    }.getOrDefault(0L)
}
