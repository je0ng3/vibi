package com.vibi.shared.data.repository

import com.vibi.shared.data.remote.api.BffApi
import com.vibi.shared.domain.repository.AutoSubtitleRepository
import com.vibi.shared.domain.repository.AutoSubtitleStatus

/**
 * v1 stub — vibi 는 iOS 우선 배포라 Android 런타임 사용 X.
 */
class AutoSubtitleRepositoryImpl(
    @Suppress("unused") private val api: BffApi,
    @Suppress("unused") private val uploader: MediaJobUploader,
) : AutoSubtitleRepository {

    override suspend fun submit(
        sourceUri: String,
        mediaType: String,
        sourceLanguageCode: String,
        targetLanguageCodes: List<String>,
        numberOfSpeakers: Int,
        editedRenderJobId: String?,
    ): Result<String> = error("Android not supported in v1")

    override suspend fun regenerate(
        srtBytes: ByteArray,
        sourceLanguageCode: String,
        targetLanguageCodes: List<String>,
    ): Result<String> = error("Android not supported in v1")

    override suspend fun pollStatus(jobId: String): Result<AutoSubtitleStatus> =
        error("Android not supported in v1")

    override suspend fun fetchSrt(srtUrl: String): Result<String> =
        error("Android not supported in v1")
}
