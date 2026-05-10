package com.vibi.shared.domain.repository

import com.vibi.shared.domain.model.DubClip
import kotlinx.coroutines.flow.Flow

interface DubClipRepository {
    fun observeClips(projectId: String): Flow<List<DubClip>>
    suspend fun addClip(clip: DubClip)
    suspend fun addClips(clips: List<DubClip>)
    suspend fun updateClip(clip: DubClip)
    suspend fun deleteClip(clipId: String)
    suspend fun deleteAllClips(projectId: String)
    suspend fun getClip(clipId: String): DubClip?
}
