package com.vibi.shared.di

import com.vibi.shared.domain.usecase.bgm.AddBgmClipUseCase
import com.vibi.shared.domain.usecase.bgm.UpdateBgmClipUseCase
import com.vibi.shared.domain.usecase.draft.ExpireOldDraftsUseCase
import com.vibi.shared.domain.usecase.image.AddImageClipUseCase
import com.vibi.shared.domain.usecase.image.UpdateImageClipUseCase
import com.vibi.shared.domain.usecase.input.CreateProjectWithInitialVideoSegmentUseCase
import com.vibi.shared.domain.usecase.input.SetProjectFrameUseCase
import com.vibi.shared.domain.usecase.input.ValidateVideoUseCase
import com.vibi.shared.domain.usecase.render.EnsureLatestRenderUseCase
import com.vibi.shared.domain.usecase.save.ListExportVariantsUseCase
import com.vibi.shared.domain.usecase.save.SaveAllVariantsUseCase
import com.vibi.shared.domain.usecase.separation.PollSeparationUseCase
import com.vibi.shared.domain.usecase.separation.StartAudioSeparationUseCase
import com.vibi.shared.domain.usecase.text.AddTextOverlayUseCase
import com.vibi.shared.domain.usecase.text.DuplicateTextOverlayUseCase
import com.vibi.shared.domain.usecase.text.UpdateTextOverlayUseCase
import com.vibi.shared.domain.usecase.timeline.AddImageSegmentUseCase
import com.vibi.shared.domain.usecase.timeline.AddVideoSegmentUseCase
import com.vibi.shared.domain.usecase.timeline.DuplicateSegmentRangeUseCase
import com.vibi.shared.domain.usecase.timeline.RemoveSegmentRangeUseCase
import com.vibi.shared.domain.usecase.timeline.RemoveSegmentUseCase
import com.vibi.shared.domain.usecase.timeline.SplitSegmentUseCase
import com.vibi.shared.domain.usecase.timeline.UpdateImageSegmentDurationUseCase
import com.vibi.shared.domain.usecase.timeline.UpdateImageSegmentPositionUseCase
import com.vibi.shared.domain.usecase.timeline.UpdateSegmentSpeedUseCase
import com.vibi.shared.domain.usecase.timeline.UpdateSegmentTrimUseCase
import com.vibi.shared.domain.usecase.timeline.UpdateSegmentVolumeUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
    // input
    factoryOf(::ValidateVideoUseCase)
    factoryOf(::CreateProjectWithInitialVideoSegmentUseCase)
    factoryOf(::SetProjectFrameUseCase)

    // draft (메인 화면 "이어서 작업" 만료 cleanup)
    factory { ExpireOldDraftsUseCase(get()) }

    // image
    factoryOf(::AddImageClipUseCase)
    factoryOf(::UpdateImageClipUseCase)

    // save (timeline header → 모든 variant 렌더 + 갤러리 저장)
    factory {
        SaveAllVariantsUseCase(
            platformAdapter = get(),
            gallerySaver = get(),
            editProjectRepository = get(),
            segmentRepository = get(),
            bgmClipRepository = get(),
            separationDirectiveRepository = get(),
            bffApi = get(),
        )
    }
    factoryOf(::ListExportVariantsUseCase)

    // separation
    factoryOf(::StartAudioSeparationUseCase)
    factoryOf(::PollSeparationUseCase)

    // render — 음원분리 시작 직전 편집 영상 source 보장
    factoryOf(::EnsureLatestRenderUseCase)

    // bgm
    factoryOf(::AddBgmClipUseCase)
    factoryOf(::UpdateBgmClipUseCase)

    // text
    factoryOf(::AddTextOverlayUseCase)
    factoryOf(::UpdateTextOverlayUseCase)
    factoryOf(::DuplicateTextOverlayUseCase)

    // timeline
    factoryOf(::AddVideoSegmentUseCase)
    factoryOf(::AddImageSegmentUseCase)
    factoryOf(::RemoveSegmentUseCase)
    factoryOf(::RemoveSegmentRangeUseCase)
    factoryOf(::DuplicateSegmentRangeUseCase)
    factoryOf(::SplitSegmentUseCase)
    factoryOf(::UpdateSegmentTrimUseCase)
    factoryOf(::UpdateSegmentVolumeUseCase)
    factoryOf(::UpdateSegmentSpeedUseCase)
    factoryOf(::UpdateImageSegmentDurationUseCase)
    factoryOf(::UpdateImageSegmentPositionUseCase)
}
