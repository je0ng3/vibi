package com.vibi.shared.domain.usecase.input

import com.vibi.shared.domain.model.EditProject
import com.vibi.shared.domain.model.Segment
import com.vibi.shared.domain.model.SegmentType
import com.vibi.shared.domain.model.VideoInfo
import com.vibi.shared.domain.repository.EditProjectRepository
import com.vibi.shared.platform.currentTimeMillis
import com.vibi.shared.platform.generateId

class CreateProjectWithInitialVideoSegmentUseCase constructor(
    private val editProjectRepository: EditProjectRepository
) {
    suspend operator fun invoke(videoInfo: VideoInfo): String {
        val projectId = generateId()
        val now = currentTimeMillis()
        val project = EditProject(
            projectId = projectId,
            createdAt = now,
            updatedAt = now,
            frameWidth = videoInfo.width,
            frameHeight = videoInfo.height,
        )
        val segment = Segment(
            id = "${projectId}_seg0",
            projectId = projectId,
            type = SegmentType.VIDEO,
            order = 0,
            sourceUri = videoInfo.uri,
            durationMs = videoInfo.durationMs,
            width = videoInfo.width,
            height = videoInfo.height
        )
        editProjectRepository.createProjectWithSegment(project, segment)
        return projectId
    }
}
