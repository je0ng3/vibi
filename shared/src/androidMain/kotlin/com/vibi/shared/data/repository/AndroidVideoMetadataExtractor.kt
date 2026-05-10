package com.vibi.shared.data.repository

import android.content.Context
import com.vibi.shared.domain.model.VideoInfo
import com.vibi.shared.domain.usecase.input.VideoMetadataExtractor

/**
 * v1 stub — vibi 는 iOS 우선 배포라 Android 런타임 사용 X.
 */
class AndroidVideoMetadataExtractor constructor(
    @Suppress("unused") private val context: Context,
) : VideoMetadataExtractor {

    override suspend fun extract(uri: String): VideoInfo? =
        error("Android not supported in v1")
}
