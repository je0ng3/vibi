package com.vibi.shared.data.remote

import com.russhwolf.settings.Settings
import com.vibi.shared.data.remote.api.BffApi
import com.vibi.shared.data.remote.dto.AssetUploadUrlRequest
import com.vibi.shared.platform.FileStat
import com.vibi.shared.platform.readFileBytes
import com.vibi.shared.platform.sha256HexOfFile
import com.vibi.shared.platform.statFile

/**
 * 로컬 파일 → R2 자산 키 매핑 캐시. `(path, size, mtime)` 가 같으면 같은 파일로 간주하고
 * sha256 재계산을 건너뛴다. server-side dedup 확인은 [AssetUploadManager] 가 매번 BFF 호출로
 * 안전망 — 캐시는 sha256 작업만 절약.
 */
class AssetKeyCache(private val settings: Settings) {
    fun get(path: String, meta: FileStat): String? =
        settings.getStringOrNull(keyFor(path, meta))

    fun put(path: String, meta: FileStat, assetKey: String) {
        settings.putString(keyFor(path, meta), assetKey)
    }

    private fun keyFor(path: String, meta: FileStat): String =
        "asset_v1:$path|${meta.sizeBytes}|${meta.lastModifiedMs}"
}

/**
 * 로컬 파일을 R2 에 업로드 보장하고 BFF 가 RenderConfigV3 에 넣을 assetKey 를 반환.
 *
 * 흐름:
 *   1) 로컬 [AssetKeyCache] hit → 즉시 반환 (sha256/네트워크 호출 모두 skip)
 *   2) sha256 + size + ext + contentType 으로 BFF 에 upload-url 요청
 *   3) `alreadyExists=true` 면 모바일은 PUT skip — 그 assetKey 만 캐시 박고 반환
 *   4) 그렇지 않으면 presigned URL 로 R2 직접 PUT → 성공 시 캐시 저장
 *
 * R2 가 lifecycle 만료로 객체를 지웠어도 매 호출이 [BffApi.requestAssetUploadUrl] 을 통과하므로
 * `alreadyExists=false` 분기에서 자연 재업로드.
 */
class AssetUploadManager(
    private val api: BffApi,
    private val cache: AssetKeyCache,
) {
    suspend fun ensureUploaded(
        localPath: String,
        ext: String,
        contentType: String,
    ): String {
        val meta = statFile(localPath)
        cache.get(localPath, meta)?.let { return it }

        val sha = sha256HexOfFile(localPath)
        val resp = api.requestAssetUploadUrl(
            AssetUploadUrlRequest(
                sha256Hex = sha,
                sizeBytes = meta.sizeBytes,
                ext = ext,
                contentType = contentType,
            )
        )
        if (!resp.alreadyExists) {
            val url = requireNotNull(resp.uploadUrl) {
                "BFF returned alreadyExists=false but uploadUrl=null"
            }
            val bytes = readFileBytes(localPath)
            val ok = api.putAssetToR2(url, bytes, contentType)
            require(ok) { "R2 PUT failed for $localPath (key=${resp.assetKey})" }
        }
        cache.put(localPath, meta, resp.assetKey)
        return resp.assetKey
    }
}
