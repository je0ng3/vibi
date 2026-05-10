package com.vibi.shared.domain.usecase.input

import com.vibi.shared.domain.model.ImageInfo

interface ImageMetadataExtractor {
    suspend fun extract(uri: String): ImageInfo?
}
