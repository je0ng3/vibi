package com.vibi.shared.data.repository

import android.content.Context
import com.vibi.shared.domain.usecase.share.GallerySaver

/**
 * v1 stub — vibi 는 iOS 우선 배포라 Android 런타임 사용 X.
 */
class AndroidGallerySaver constructor(
    @Suppress("unused") private val context: Context,
) : GallerySaver {

    override suspend fun saveVideo(sourcePath: String, displayName: String): Result<Unit> =
        error("Android not supported in v1")
}
