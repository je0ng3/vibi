package com.vibi.cmp.platform

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.vibi.shared.platform.stripFileScheme
import java.io.File

/**
 * :cmp androidMain 공용 미디어 resolve 헬퍼 — 저장된 source 문자열을 media3 가 재생 가능한 [Uri] 로,
 * content:// 의 사용자 파일명을 조회. StemMixer / BgmPlaybackSync / MediaPicker / AudioPicker 재사용.
 */

/**
 * 저장 source 문자열 → [Uri]. content:// / http(s) 는 그대로 parse, file:// / 절대경로는 [Uri.fromFile]
 * ([stripFileScheme] 로 %-디코드). BFF-상대(`/api/…`) 경로가 오면 호출측이 미리 절대 URL 로 만든 뒤 넘긴다.
 */
internal fun resolveMediaUri(stored: String): Uri =
    if (stored.startsWith("content://") ||
        stored.startsWith("http://") ||
        stored.startsWith("https://")
    ) {
        Uri.parse(stored)
    } else {
        Uri.fromFile(File(stripFileScheme(stored)))
    }

/** content:// 의 사용자 보이는 파일명(OpenableColumns.DISPLAY_NAME). 없으면 null. */
internal fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? = runCatching {
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
    }
}.getOrNull()
