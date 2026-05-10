package com.vibi.shared.data.repository

import android.content.Context
import com.vibi.shared.domain.model.ImageInfo
import com.vibi.shared.domain.usecase.input.ImageMetadataExtractor

/**
 * v1 stub — vibi 는 iOS 우선 배포라 Android 런타임 사용 X.
 */
class AndroidImageMetadataExtractor constructor(
    @Suppress("unused") private val context: Context,
) : ImageMetadataExtractor {

    override suspend fun extract(uri: String): ImageInfo? =
        error("Android not supported in v1")
}
