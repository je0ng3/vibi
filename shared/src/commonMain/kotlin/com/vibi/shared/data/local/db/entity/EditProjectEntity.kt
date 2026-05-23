package com.vibi.shared.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vibi.shared.domain.model.EditProject

@Entity(tableName = "edit_projects")
data class EditProjectEntity(
    @PrimaryKey val projectId: String,
    /** JWT sub claim. 같은 기기에 다른 계정이 로그인했을 때 이전 작업 노출 방지용 스코핑 키. */
    val userId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val title: String? = null,
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    val backgroundColorHex: String = EditProject.DEFAULT_BACKGROUND_COLOR_HEX,
    val videoScale: Float = EditProject.DEFAULT_VIDEO_SCALE,
    val videoOffsetXPct: Float = 0f,
    val videoOffsetYPct: Float = 0f,
    val separationJobId: String? = null,
    val separationSegmentId: String? = null,
    val separationNumberOfSpeakers: Int = 2,
    val separationMuteOriginal: Boolean = true,
    val separationStatus: String = "IDLE",
    val separationError: String? = null,
    /**
     * JSON array — 동시 진행 중 음원분리 잡들. 빈 문자열이면 비어있음 (legacy 데이터 호환).
     * 항목 스키마: PersistedSeparationJobDto (jobId, segmentId, rangeStartMs?, rangeEndMs?,
     * numberOfSpeakers, muteOriginalSegmentAudio).
     */
    val processingSeparationsJson: String = "",
)
