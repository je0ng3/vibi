package com.vibi.shared.domain.usecase.save

import com.vibi.shared.domain.model.BgmClip
import com.vibi.shared.domain.model.EditProject
import com.vibi.shared.domain.model.ImageClip
import com.vibi.shared.domain.model.Segment
import com.vibi.shared.domain.model.SegmentType
import com.vibi.shared.domain.model.SeparationDirective
import com.vibi.shared.domain.model.TextOverlay
import com.vibi.shared.domain.repository.BgmClipRepository
import com.vibi.shared.domain.repository.EditProjectRepository
import com.vibi.shared.domain.repository.ImageClipRepository
import com.vibi.shared.domain.repository.SegmentRepository
import com.vibi.shared.domain.repository.SeparationDirectiveRepository
import com.vibi.shared.domain.repository.TextOverlayRepository
import com.vibi.shared.domain.usecase.share.GallerySaver
import com.vibi.shared.ui.export.ExportPlatformAdapter
import com.vibi.shared.ui.export.ExportRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SaveAllVariantsUseCaseTest {

    @Test
    fun `noEdits 단일 VIDEO 는 bypass — render skip 후 source 직접 저장`() = runTest {
        val source = "file:///tmp/video.mp4"
        val segment = videoSegment(sourceUri = source)
        val useCase = useCaseWith(
            project = projectFor(segment),
            segments = listOf(segment),
        )
        val progress = mutableListOf<Int>()

        val result = useCase(projectId = "p1", onProgress = progress::add)

        assertTrue(result.isSuccess)
        val saved = result.getOrThrow()
        assertEquals(1, saved.size)
        assertEquals(source, saved[0].outputPath, "bypass 시 source URI 그대로 전달돼야 함")
        assertEquals(listOf(0, 100, 100), progress, "0 → 100 (copy) → 100 (final)")
    }

    @Test
    fun `volumeScale 변경 시 bypass 거부 — BFF render 경로`() = runTest {
        val segment = videoSegment(volumeScale = 0.5f)
        val adapter = FakeExportPlatformAdapter(success = "/tmp/rendered.mp4")
        val useCase = useCaseWith(
            project = projectFor(segment),
            segments = listOf(segment),
            adapter = adapter,
        )

        val result = useCase(projectId = "p1", onProgress = { })

        assertTrue(result.isSuccess)
        assertEquals(1, adapter.callCount, "edit 있으므로 render 1회 호출되어야 함")
        assertEquals("/tmp/rendered.mp4", result.getOrThrow()[0].outputPath)
    }

    @Test
    fun `trimEndMs equals durationMs 는 untrimmed sentinel 로 인정 — bypass 진행`() = runTest {
        // Segment.hasNonTrivialEdits 의 SSOT 헬퍼가 trimEndMs == durationMs 를 미트림으로 본다.
        val segment = videoSegment(durationMs = 10_000L, trimEndMs = 10_000L)
        val adapter = FakeExportPlatformAdapter(success = "/should/not/be/used.mp4")
        val useCase = useCaseWith(
            project = projectFor(segment),
            segments = listOf(segment),
            adapter = adapter,
        )

        val result = useCase(projectId = "p1", onProgress = { })

        assertTrue(result.isSuccess)
        assertEquals(0, adapter.callCount, "untrimmed sentinel 이므로 render 호출되면 안 됨")
    }

    @Test
    fun `BGM 있으면 bypass 거부`() = runTest {
        val segment = videoSegment()
        val bgm = BgmClip(
            id = "b1", projectId = "p1", sourceUri = "/tmp/bg.mp3",
            startMs = 0L, sourceDurationMs = 5_000L, lane = 0,
        )
        val adapter = FakeExportPlatformAdapter(success = "/tmp/rendered.mp4")
        val useCase = useCaseWith(
            project = projectFor(segment),
            segments = listOf(segment),
            bgmClips = listOf(bgm),
            adapter = adapter,
        )

        val result = useCase(projectId = "p1", onProgress = { })

        assertTrue(result.isSuccess)
        assertEquals(1, adapter.callCount)
    }

    @Test
    fun `textOverlay 추가는 bypass 막지 않음 — render 도 처리 안 함 preview 전용`() = runTest {
        // isProjectEdited 도메인 SSOT 가 명시: image/text overlay 는 export 출력에 영향 없음.
        val segment = videoSegment()
        val overlay = TextOverlay(
            id = "t1", projectId = "p1", text = "hi",
            startMs = 0L, endMs = 1_000L, lane = 0,
            fontFamily = "Default", fontSizeSp = 16f,
            colorHex = "#FFFFFF",
        )
        val adapter = FakeExportPlatformAdapter(success = "/tmp/rendered.mp4")
        val useCase = useCaseWith(
            project = projectFor(segment),
            segments = listOf(segment),
            textOverlays = listOf(overlay),
            adapter = adapter,
        )

        val result = useCase(projectId = "p1", onProgress = { })

        assertTrue(result.isSuccess)
        assertEquals(0, adapter.callCount, "overlay 는 bypass 가드에서 무시 — render skip")
    }

    @Test
    fun `render 실패 시 Result_failure 반환 + Throwable 보존`() = runTest {
        val segment = videoSegment(volumeScale = 0.5f) // render 강제
        val adapter = FakeExportPlatformAdapter(failure = RuntimeException("bff timeout"))
        val useCase = useCaseWith(
            project = projectFor(segment),
            segments = listOf(segment),
            adapter = adapter,
        )

        val result = useCase(projectId = "p1", onProgress = { })

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Render failed") == true)
    }

    @Test
    fun `gallerySaver 실패 시 Result_failure`() = runTest {
        val segment = videoSegment()
        val saver = FakeGallerySaver(failure = RuntimeException("permission denied"))
        val useCase = useCaseWith(
            project = projectFor(segment),
            segments = listOf(segment),
            gallerySaver = saver,
        )

        val result = useCase(projectId = "p1", onProgress = { })

        assertTrue(result.isFailure)
    }

    @Test
    fun `saveToGallery=false 면 gallerySaver 호출 안 함`() = runTest {
        val segment = videoSegment()
        val saver = FakeGallerySaver()
        val useCase = useCaseWith(
            project = projectFor(segment),
            segments = listOf(segment),
            gallerySaver = saver,
        )

        val result = useCase(projectId = "p1", onProgress = { }, saveToGallery = false)

        assertTrue(result.isSuccess)
        assertEquals(0, saver.callCount)
    }

    @Test
    fun `CancellationException 은 rethrow — 구조적 동시성 보존`() = runTest {
        val segment = videoSegment(volumeScale = 0.5f)
        val adapter = FakeExportPlatformAdapter(failure = CancellationException("cancelled"))
        val useCase = useCaseWith(
            project = projectFor(segment),
            segments = listOf(segment),
            adapter = adapter,
        )

        assertFailsWith<CancellationException> {
            useCase(projectId = "p1", onProgress = { })
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun videoSegment(
        id: String = "s0",
        sourceUri: String = "file:///tmp/v.mp4",
        durationMs: Long = 10_000L,
        trimStartMs: Long = 0L,
        trimEndMs: Long = 0L,
        volumeScale: Float = 1.0f,
        speedScale: Float = 1.0f,
        width: Int = 1920,
        height: Int = 1080,
    ) = Segment(
        id = id, projectId = "p1", type = SegmentType.VIDEO, order = 0,
        sourceUri = sourceUri, durationMs = durationMs,
        width = width, height = height,
        trimStartMs = trimStartMs, trimEndMs = trimEndMs,
        volumeScale = volumeScale, speedScale = speedScale,
    )

    private fun projectFor(seg: Segment) = EditProject(
        projectId = "p1", createdAt = 0L, updatedAt = 0L,
        frameWidth = seg.width, frameHeight = seg.height,
    )

    private fun useCaseWith(
        project: EditProject,
        segments: List<Segment>,
        bgmClips: List<BgmClip> = emptyList(),
        separationDirectives: List<SeparationDirective> = emptyList(),
        textOverlays: List<TextOverlay> = emptyList(),
        imageClips: List<ImageClip> = emptyList(),
        adapter: FakeExportPlatformAdapter = FakeExportPlatformAdapter(success = "/tmp/rendered.mp4"),
        gallerySaver: FakeGallerySaver = FakeGallerySaver(),
    ): SaveAllVariantsUseCase = SaveAllVariantsUseCase(
        platformAdapter = adapter,
        gallerySaver = gallerySaver,
        editProjectRepository = FakeEditProjectRepo(project),
        segmentRepository = FakeSegmentRepo(segments),
        bgmClipRepository = FakeBgmRepo(bgmClips),
        separationDirectiveRepository = FakeSeparationRepo(separationDirectives),
    )

    // ── fakes (textOverlay/imageClip 는 use case ctor 에 빠졌으므로 미러용 필요 없음) ──

    private class FakeExportPlatformAdapter(
        private val success: String? = null,
        private val failure: Throwable? = null,
    ) : ExportPlatformAdapter {
        var callCount = 0
        override suspend fun executeExport(
            request: ExportRequest,
            onProgress: (percent: Int) -> Unit,
        ): Result<String> {
            callCount++
            // Contract: executeExport 는 Result<String> 반환. CancellationException 만 throw.
            if (failure != null) {
                if (failure is CancellationException) throw failure
                return Result.failure(failure)
            }
            return Result.success(success ?: error("no fake response configured"))
        }
    }

    private class FakeGallerySaver(
        private val failure: Throwable? = null,
    ) : GallerySaver {
        var callCount = 0
        override suspend fun saveVideo(sourcePath: String, displayName: String): Result<Unit> {
            callCount++
            return failure?.let { Result.failure(it) } ?: Result.success(Unit)
        }
    }

    private class FakeEditProjectRepo(private val project: EditProject) : EditProjectRepository {
        override suspend fun createProject(project: EditProject) {}
        override suspend fun createProjectWithSegment(project: EditProject, segment: Segment) {}
        override fun observeProject(projectId: String): Flow<EditProject?> = flowOf(project)
        override suspend fun getProject(projectId: String): EditProject? = project
        override suspend fun updateProject(project: EditProject, touchActivity: Boolean) {}
        override suspend fun deleteProject(projectId: String) {}
        override fun observeAllProjects(): Flow<List<EditProject>> = flowOf(listOf(project))
        override suspend fun expireOldDrafts(thresholdMs: Long) {}
    }

    private class FakeSegmentRepo(private val segments: List<Segment>) : SegmentRepository {
        override fun observeByProjectId(projectId: String): Flow<List<Segment>> = flowOf(segments)
        override suspend fun getByProjectId(projectId: String): List<Segment> = segments
        override suspend fun getSegment(id: String): Segment? = segments.firstOrNull { it.id == id }
        override suspend fun addSegment(segment: Segment) {}
        override suspend fun addSegments(segments: List<Segment>) {}
        override suspend fun updateSegment(segment: Segment) {}
        override suspend fun deleteSegment(id: String) {}
        override suspend fun deleteAllByProjectId(projectId: String) {}
        override suspend fun getMaxOrder(projectId: String): Int = segments.maxOfOrNull { it.order } ?: -1
        override suspend fun getFirstSourceUri(projectId: String): String? = segments.firstOrNull()?.sourceUri
    }

    private class FakeBgmRepo(private val clips: List<BgmClip>) : BgmClipRepository {
        override fun observeClips(projectId: String): Flow<List<BgmClip>> = flowOf(clips)
        override suspend fun getClip(clipId: String): BgmClip? = clips.firstOrNull { it.id == clipId }
        override suspend fun addClip(clip: BgmClip) {}
        override suspend fun addClips(clips: List<BgmClip>) {}
        override suspend fun updateClip(clip: BgmClip) {}
        override suspend fun deleteClip(clipId: String) {}
        override suspend fun deleteAllByProjectId(projectId: String) {}
    }

    private class FakeSeparationRepo(private val directives: List<SeparationDirective>) : SeparationDirectiveRepository {
        override suspend fun add(directive: SeparationDirective) {}
        override suspend fun addAll(directives: List<SeparationDirective>) {}
        override fun observe(projectId: String): Flow<List<SeparationDirective>> = flowOf(directives)
        override suspend fun getByProject(projectId: String): List<SeparationDirective> = directives
        override suspend fun delete(id: String) {}
        override suspend fun deleteByProject(projectId: String) {}
    }
}
