package com.vibi.shared.data.repository

import android.content.Context
import com.vibi.shared.data.remote.api.BffApi
import com.vibi.shared.domain.repository.AutoDubJobStatus
import com.vibi.shared.domain.repository.AutoDubRepository

/**
 * v1 stub — vibi 는 iOS 우선 배포라 Android 런타임 사용 X.
 */
class AutoDubRepositoryImpl(
    @Suppress("unused") private val api: BffApi,
    @Suppress("unused") private val uploader: MediaJobUploader,
    @Suppress("unused") private val context: Context,
) : AutoDubRepository {

    override suspend fun submit(
        sourceUri: String,
        mediaType: String,
        sourceLanguageCode: String,
        targetLanguageCode: String,
        numberOfSpeakers: Int,
        ttsModel: String?,
        editedRenderJobId: String?,
    ): Result<String> = error("Android not supported in v1")

    override suspend fun pollStatus(jobId: String): Result<AutoDubJobStatus> =
        error("Android not supported in v1")

    override suspend fun downloadDubbedAudio(
        audioUrl: String,
        outputFileName: String,
    ): Result<String> = error("Android not supported in v1")

    override suspend fun downloadDubbedVideo(
        videoUrl: String,
        outputFileName: String,
    ): Result<String> = error("Android not supported in v1")
}
