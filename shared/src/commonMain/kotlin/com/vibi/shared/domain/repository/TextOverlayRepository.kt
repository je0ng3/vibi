package com.vibi.shared.domain.repository

import com.vibi.shared.domain.model.TextOverlay
import kotlinx.coroutines.flow.Flow

interface TextOverlayRepository {
    fun observeOverlays(projectId: String): Flow<List<TextOverlay>>
    suspend fun getOverlay(overlayId: String): TextOverlay?
    suspend fun addOverlay(overlay: TextOverlay)
    suspend fun addOverlays(overlays: List<TextOverlay>)
    suspend fun updateOverlay(overlay: TextOverlay)
    suspend fun deleteOverlay(overlayId: String)
    suspend fun deleteAllByProjectId(projectId: String)
}
