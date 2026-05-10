package com.vibi.shared.platform

import android.content.Context

/**
 * v1 stub — vibi 는 iOS 우선 배포라 Android 런타임 사용 X.
 */
class AndroidVideoThumbnailExtractor constructor(
    @Suppress("unused") private val context: Context,
) : VideoThumbnailExtractor {

    override suspend fun extractThumbnail(uri: String, atMs: Long): String? =
        error("Android not supported in v1")
}
