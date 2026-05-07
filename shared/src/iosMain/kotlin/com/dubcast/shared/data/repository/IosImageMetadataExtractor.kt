package com.dubcast.shared.data.repository

import com.dubcast.shared.domain.model.ImageInfo
import com.dubcast.shared.domain.usecase.input.ImageMetadataExtractor
import com.dubcast.shared.platform.resolveStoredUriToPath
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIImage

class IosImageMetadataExtractor : ImageMetadataExtractor {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun extract(uri: String): ImageInfo? {
        // resolver 가 상대 / 절대 / file:// / 옛 UUID remap 모두 처리.
        val path = resolveStoredUriToPath(uri) ?: return null
        val image = UIImage.imageWithContentsOfFile(path) ?: return null
        val (width, height) = image.size.useContents { Pair(width.toInt(), height.toInt()) }
        if (width <= 0 || height <= 0) return null
        return ImageInfo(uri = uri, width = width, height = height)
    }
}
