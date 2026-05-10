package com.vibi.shared.domain.usecase.input

import com.vibi.shared.domain.model.VideoInfo

interface VideoMetadataExtractor {
    suspend fun extract(uri: String): VideoInfo?
}
