package com.vibi.shared.domain.usecase.save

import com.vibi.shared.data.remote.AssetUploader

/**
 * 편집 진입 시점에 영상 원본을 R2 에 미리 업로드(prewarm)해, 저장/공유 시점의 렌더 업로드 대기를
 * 없앤다.
 *
 * [AssetUploader.ensureUploaded] 는 파일 해시 기반 멱등 연산이라, 여기서 미리 호출해도 산출물에는
 * 영향이 없다 — R2 와 로컬 [com.vibi.shared.data.remote.AssetKeyCache] 만 채워둘 뿐이다. 이후
 * 저장 경로([com.vibi.shared.data.repository.V3RenderExecutor])가 같은 파일을 다시 올리려 하면
 * 캐시 히트로 즉시 같은 assetKey 를 받는다. 따라서 prewarm 은 순수하게 대기 시간만 줄인다.
 *
 * **Best-effort** — 네트워크 실패·파일 누락 등 어떤 이유로 prewarm 이 실패해도 throw 하지 않는다.
 * 실패하면 저장 시점에 평소대로 업로드되므로 회귀가 없다. 호출자는 별도 코루틴에서 fire-and-forget
 * 하면 된다.
 */
class PrewarmAssetUploadUseCase(
    private val uploader: AssetUploader,
) {
    /**
     * [videoPaths] 의 distinct 경로마다 [AssetUploader.ensureUploaded] 를 호출한다. v1 은 영상
     * 세그먼트만 대상 — BGM 은 보통 작아 효과가 낮아 후속 버전으로 미룬다.
     */
    suspend operator fun invoke(videoPaths: List<String>) {
        for (path in videoPaths.filter { it.isNotBlank() }.distinct()) {
            runCatching {
                uploader.ensureUploaded(
                    localPath = path,
                    ext = "mp4",
                    contentType = "video/mp4",
                )
            }.onFailure { e ->
                // best-effort — 저장 시점에 자연 재시도되므로 삼킨다.
                println("[PrewarmAssetUpload] skip $path: ${e.message}")
            }
        }
    }
}
