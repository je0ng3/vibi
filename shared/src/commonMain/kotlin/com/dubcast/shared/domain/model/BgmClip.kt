package com.dubcast.shared.domain.model

data class BgmClip(
    val id: String,
    val projectId: String,
    val sourceUri: String,
    val sourceDurationMs: Long,
    val startMs: Long,
    val volumeScale: Float = 1.0f,
    val speedScale: Float = 1.0f,
    /**
     * Visual lane (row) index in the BGM timeline lane group. 0 = top lane.
     *
     * 시간상 겹치는 BGM 클립을 위·아래 별도 행으로 시각 분리하기 위함. 사용자가 클립을
     * vertical drag 하면 ViewModel 의 in-memory override map 으로 갱신되며, 이 값이
     * 그 결과를 반영한다. **현재는 DB 영속화 없음** — repository → entity 매핑은 lane 을
     * 무시하고, ViewModel 이 매번 observe 시 override 를 다시 적용한다. 후속 마이그레이션
     * 단계에서 `bgm_clips.lane` 컬럼 + Room migration v33 으로 영속화 예정.
     */
    val lane: Int = 0,
) {
    /** 속도 적용된 timeline 상 길이 — 음원분리 stems 도 동일 속도로 재생됨. */
    val effectiveDurationMs: Long
        get() = if (speedScale > 0f) (sourceDurationMs / speedScale).toLong() else sourceDurationMs

    companion object {
        const val MIN_VOLUME = 0f
        const val MAX_VOLUME = 2f
        const val MIN_SPEED = 0.25f
        const val MAX_SPEED = 4f
    }
}
