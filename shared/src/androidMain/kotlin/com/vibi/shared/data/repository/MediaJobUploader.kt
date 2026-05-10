package com.vibi.shared.data.repository

import android.content.Context
import com.vibi.shared.data.remote.api.BinaryPart

/**
 * v1 stub — vibi 는 iOS 우선 배포라 Android 런타임 사용 X.
 */
class MediaJobUploader(@Suppress("unused") private val context: Context) {

    suspend fun loadAsBinaryPart(
        sourceUri: String,
        mediaType: String,
        prefix: String,
    ): BinaryPart = error("Android not supported in v1")
}
