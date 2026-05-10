package com.vibi.shared.data.repository

import android.content.Context
import com.vibi.shared.domain.usecase.input.AudioInfo
import com.vibi.shared.domain.usecase.input.AudioMetadataExtractor

/**
 * v1 stub — vibi 는 iOS 우선 배포라 Android 런타임 사용 X.
 */
class AndroidAudioMetadataExtractor constructor(
    @Suppress("unused") private val context: Context,
) : AudioMetadataExtractor {

    override suspend fun extract(uri: String): AudioInfo? =
        error("Android not supported in v1")
}
