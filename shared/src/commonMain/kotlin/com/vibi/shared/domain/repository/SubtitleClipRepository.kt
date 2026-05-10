package com.vibi.shared.domain.repository

import com.vibi.shared.domain.model.SubtitleClip
import kotlinx.coroutines.flow.Flow

interface SubtitleClipRepository {
    fun observeClips(projectId: String): Flow<List<SubtitleClip>>
    suspend fun addClip(clip: SubtitleClip)
    suspend fun addClips(clips: List<SubtitleClip>)
    suspend fun updateClip(clip: SubtitleClip)
    suspend fun deleteClip(clipId: String)
    suspend fun deleteAllClips(projectId: String)
    suspend fun getClip(clipId: String): SubtitleClip?
    suspend fun deleteClipsBySourceDubClipId(dubClipId: String)
    suspend fun deleteAutoSubtitles(projectId: String)
}
