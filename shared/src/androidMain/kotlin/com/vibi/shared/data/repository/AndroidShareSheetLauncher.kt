package com.vibi.shared.data.repository

import android.content.Context
import com.vibi.shared.domain.usecase.share.ShareSheetLauncher

/**
 * v1 stub — vibi 는 iOS 우선 배포라 Android 런타임 사용 X.
 * 컴파일 통과만 보장. 호출되면 즉시 throw.
 */
class AndroidShareSheetLauncher(
    @Suppress("unused") private val context: Context,
) : ShareSheetLauncher {

    override suspend fun shareVideo(
        sourcePath: String,
        mimeType: String,
        title: String?,
    ): Result<Unit> = error("Android not supported in v1")

    override suspend fun shareVideos(
        sourcePaths: List<String>,
        mimeType: String,
        title: String?,
    ): Result<Unit> = error("Android not supported in v1")
}
