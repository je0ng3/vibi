package com.vibi.shared.platform

import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.vibi.shared.data.local.db.applicationContext
import java.io.File
import java.io.InputStream

/**
 * androidMain 공용 미디어 I/O 헬퍼 — content:// / file:// / 절대경로 처리를 한 곳에 모아
 * Hashing / GallerySaver / ShareSheet / AudioExtractor / WaveformExtractor 등에서 재사용한다.
 * (:cmp 는 `api(project(":shared"))` 라 public 심볼을 그대로 쓴다.)
 */

/** content:// / file:// 는 contentResolver, 그 외 절대경로는 File 로 스트리밍 InputStream 을 연다. */
internal fun openInputFor(pathOrUri: String): InputStream =
    if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
        requireNotNull(applicationContext.contentResolver.openInputStream(Uri.parse(pathOrUri))) {
            "Cannot open input stream for $pathOrUri"
        }
    } else {
        File(pathOrUri).inputStream()
    }

/**
 * "file://…" 를 실제 파일 경로로 변환(%-디코드 포함 — MediaPicker 가 Uri.fromFile 로 %20 등을 인코딩해
 * 저장하므로 반드시 디코드해야 File 이 실제 파일을 가리킨다). content:// / 순수 경로는 그대로 반환.
 */
fun stripFileScheme(path: String): String =
    if (path.startsWith("file://")) (Uri.parse(path).path ?: path.removePrefix("file://")) else path

/** content:// / file:// 는 contentResolver 경유, 그 외는 파일 경로로 [MediaExtractor] source 지정. */
fun MediaExtractor.setPlatformSource(pathOrUri: String) {
    if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
        setDataSource(applicationContext, Uri.parse(pathOrUri), null)
    } else {
        setDataSource(pathOrUri)
    }
}

/** 첫 audio 트랙 인덱스(없으면 null). 분리 추출과 파형 디코딩이 같은 트랙을 쓰도록 단일 소스. */
fun MediaExtractor.firstAudioTrackIndex(): Int? {
    for (i in 0 until trackCount) {
        val mime = getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
        if (mime.startsWith("audio/")) return i
    }
    return null
}

fun MediaFormat.optInt(key: String, default: Int): Int =
    if (containsKey(key)) getInteger(key) else default

fun MediaFormat.optLong(key: String, default: Long): Long =
    if (containsKey(key)) getLong(key) else default
