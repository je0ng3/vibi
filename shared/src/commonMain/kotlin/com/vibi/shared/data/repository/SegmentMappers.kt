package com.vibi.shared.data.repository

import com.vibi.shared.data.local.db.entity.SegmentEntity
import com.vibi.shared.domain.model.Segment
import com.vibi.shared.domain.model.SegmentType

/**
 * Segment ↔ SegmentEntity 단일 매핑. 두 리포지토리(SegmentRepositoryImpl, EditProjectRepositoryImpl)가
 * 같은 변환을 쓰도록 공용화 — 컬럼 누락(volume/speed/duplicatedFromId)으로 인한 무음 데이터 유실 방지.
 */
internal fun SegmentEntity.toDomain(): Segment = Segment(
    id = id,
    projectId = projectId,
    type = runCatching { SegmentType.valueOf(type) }.getOrDefault(SegmentType.VIDEO),
    order = order,
    sourceUri = sourceUri,
    durationMs = durationMs,
    width = width,
    height = height,
    trimStartMs = trimStartMs,
    trimEndMs = trimEndMs,
    volumeScale = volumeScale,
    speedScale = speedScale,
    duplicatedFromId = duplicatedFromId,
)

internal fun Segment.toEntity(): SegmentEntity = SegmentEntity(
    id = id,
    projectId = projectId,
    type = type.name,
    order = order,
    sourceUri = sourceUri,
    durationMs = durationMs,
    width = width,
    height = height,
    trimStartMs = trimStartMs,
    trimEndMs = trimEndMs,
    volumeScale = volumeScale,
    speedScale = speedScale,
    duplicatedFromId = duplicatedFromId,
)
