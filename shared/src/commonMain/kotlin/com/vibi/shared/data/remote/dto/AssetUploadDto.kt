package com.vibi.shared.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * `POST /api/v2/assets/upload-url` request — 모바일이 로컬 파일의 sha256 + 메타데이터를 보내
 * R2 presigned PUT URL 을 받는다.
 *
 * BFF 측 DTO ([com.vibi.bff.model.AssetUploadUrlRequest]) 와 1:1.
 */
@Serializable
data class AssetUploadUrlRequest(
    /** lowercase hex, 64자. AssetUploadManager 가 파일 스트리밍 SHA-256 으로 계산. */
    val sha256Hex: String,
    val sizeBytes: Long,
    /** mp4 / mov / m4a / mp3 / wav / aac. BFF 화이트리스트 외 reject. */
    val ext: String,
    val contentType: String,
)

@Serializable
data class AssetUploadUrlResponse(
    /** R2 object key. `assets/<sha>.<ext>` 패턴 — RenderConfigV3 의 sourceAssetKey/audioAssetKey 로 사용. */
    val assetKey: String,
    /** true 면 R2 가 이미 보유 → 모바일은 PUT skip 하고 assetKey 만 재사용. */
    val alreadyExists: Boolean,
    /** alreadyExists=true 면 null. false 면 presigned PUT URL — TTL 짧음 (300s). */
    val uploadUrl: String? = null,
    val expiresInSec: Long = 0L,
)
