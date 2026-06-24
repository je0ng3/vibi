package com.vibi.shared.data.repository

import com.vibi.shared.data.local.db.dao.SeparationDirectiveDao
import com.vibi.shared.data.local.db.entity.SeparationDirectiveEntity
import com.vibi.shared.domain.model.SeparationDirective
import com.vibi.shared.domain.repository.SeparationDirectiveRepository
import com.vibi.shared.domain.repository.StemSelection
import com.vibi.shared.platform.deleteLocalFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

class SeparationDirectiveRepositoryImpl(
    private val dao: SeparationDirectiveDao
) : SeparationDirectiveRepository {

    override suspend fun add(directive: SeparationDirective) {
        dao.upsert(directive.toEntity())
    }

    override suspend fun addAll(directives: List<SeparationDirective>) {
        if (directives.isEmpty()) return
        dao.upsertAll(directives.map { it.toEntity() })
    }

    override fun observe(projectId: String): Flow<List<SeparationDirective>> =
        dao.observeByProject(projectId).map { rows -> rows.map { it.toDomain() }.backfillStemDuration() }
            .distinctUntilChanged()

    override suspend fun getByProject(projectId: String): List<SeparationDirective> =
        dao.getByProject(projectId).map { it.toDomain() }.backfillStemDuration()

    /**
     * legacy row(stemTotalDurationMs 미저장=0) 의 stem 전체 길이를, **같은 stem URL 을 공유하는 현재
     * 로드된 모든 piece** 로부터 `max(sourceOffset + durationMs)` 로 추정해 채운다. 분리 직후/분할 후처럼
     * piece 가 모두 존재할 때 정확하며, 분할→이 보정→영속화(분할 시 copy 저장) 경로로 뒤 piece 삭제에도
     * 보존된다. (뒤 piece 가 이미 영속 삭제된 legacy 상태만 미복구 — 그 경우 재분리 필요.) 신규 분리
     * (이미 값 보유) 는 no-op.
     */
    private fun List<SeparationDirective>.backfillStemDuration(): List<SeparationDirective> {
        if (all { d -> d.selections.all { it.stemTotalDurationMs > 0L } }) return this
        val byUrl = mutableMapOf<String, Long>()
        for (d in this) for (sel in d.selections) {
            val url = sel.audioUrl?.takeIf { it.isNotBlank() } ?: continue
            val end = d.sourceOffsetMs + d.durationMs
            if (end > (byUrl[url] ?: 0L)) byUrl[url] = end
        }
        if (byUrl.isEmpty()) return this
        return map { d ->
            if (d.selections.all { it.stemTotalDurationMs > 0L }) d
            else d.copy(selections = d.selections.map { sel ->
                if (sel.stemTotalDurationMs > 0L) return@map sel
                val dur = sel.audioUrl?.takeIf { it.isNotBlank() }?.let { byUrl[it] } ?: 0L
                if (dur > 0L) sel.copy(stemTotalDurationMs = dur) else sel
            })
        }
    }

    override suspend fun delete(id: String) {
        dao.getById(id)?.let { deleteStemFiles(listOf(it)) }
        dao.deleteById(id)
    }

    override suspend fun deleteByProject(projectId: String) {
        deleteStemFiles(dao.getByProject(projectId))
        dao.deleteByProject(projectId)
    }

    /** directive 들에 영구 캐시된 stem 로컬 파일을 디스크에서 제거 — 누적 방지. URL/미캐시는 무시. */
    private fun deleteStemFiles(rows: List<SeparationDirectiveEntity>) {
        rows.forEach { row ->
            row.toDomain().selections.forEach { sel ->
                sel.localPath?.takeIf { it.isNotBlank() }?.let { deleteLocalFile(it) }
            }
        }
    }

    private fun SeparationDirective.toEntity() = SeparationDirectiveEntity(
        id = id,
        projectId = projectId,
        rangeStartMs = rangeStartMs,
        rangeEndMs = rangeEndMs,
        numberOfSpeakers = numberOfSpeakers,
        muteOriginalSegmentAudio = muteOriginalSegmentAudio,
        selectionsJson = persistedJson.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(StemSelectionDto.serializer()),
            selections.map {
                StemSelectionDto(it.stemId, it.volume, it.audioUrl, it.selected, it.localPath, it.stemTotalDurationMs)
            }
        ),
        createdAt = createdAt,
        jobId = jobId,
        sourceOffsetMs = sourceOffsetMs,
        segmentId = segmentId,
        localStartMs = localStartMs,
        localEndMs = localEndMs,
    )

    private fun SeparationDirectiveEntity.toDomain(): SeparationDirective {
        val selections: List<StemSelection> = runCatching {
            persistedJson.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(StemSelectionDto.serializer()),
                selectionsJson.ifBlank { "[]" }
            ).map {
                StemSelection(it.stemId, it.volume, it.audioUrl, it.selected, it.localPath, it.stemTotalDurationMs)
            }
        }.getOrDefault(emptyList())
        return SeparationDirective(
            id = id,
            projectId = projectId,
            rangeStartMs = rangeStartMs,
            rangeEndMs = rangeEndMs,
            numberOfSpeakers = numberOfSpeakers,
            muteOriginalSegmentAudio = muteOriginalSegmentAudio,
            selections = selections,
            createdAt = createdAt,
            jobId = jobId,
            sourceOffsetMs = sourceOffsetMs,
            segmentId = segmentId,
            localStartMs = localStartMs,
            localEndMs = localEndMs,
        )
    }

    @Serializable
    private data class StemSelectionDto(
        val stemId: String,
        val volume: Float,
        val audioUrl: String? = null,
        // legacy 데이터(이 필드 없는 row)는 default true — 기존 동작 유지.
        val selected: Boolean = true,
        // 영구 캐시된 stem 로컬 경로. legacy/미캐시 row 는 null.
        val localPath: String? = null,
        // stem audio 전체 길이(ms). legacy row 는 0 → 파형이 추정으로 폴백. JSON blob 이라 DB 스키마 불변.
        val stemTotalDurationMs: Long = 0L,
    )

}
