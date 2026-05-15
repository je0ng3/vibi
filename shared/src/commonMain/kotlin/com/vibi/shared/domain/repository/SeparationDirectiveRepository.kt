package com.vibi.shared.domain.repository

import com.vibi.shared.domain.model.SeparationDirective
import kotlinx.coroutines.flow.Flow

interface SeparationDirectiveRepository {
    suspend fun add(directive: SeparationDirective)
    /** 배치 upsert — ripple 같은 N-row 갱신을 1 회 transaction + 1 회 observe emit 로. */
    suspend fun addAll(directives: List<SeparationDirective>)
    fun observe(projectId: String): Flow<List<SeparationDirective>>
    suspend fun getByProject(projectId: String): List<SeparationDirective>
    suspend fun delete(id: String)
    suspend fun deleteByProject(projectId: String)
}
