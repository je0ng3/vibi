package com.vibi.shared.domain.repository

import com.vibi.shared.domain.model.BgmClip
import kotlinx.coroutines.flow.Flow

interface BgmClipRepository {
    fun observeClips(projectId: String): Flow<List<BgmClip>>
    suspend fun getClip(clipId: String): BgmClip?
    suspend fun addClip(clip: BgmClip)
    suspend fun addClips(clips: List<BgmClip>)
    suspend fun updateClip(clip: BgmClip)
    suspend fun deleteClip(clipId: String)
    suspend fun deleteAllByProjectId(projectId: String)
}
