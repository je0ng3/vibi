package com.vibi.shared.di

import com.vibi.shared.data.repository.AndroidAudioMetadataExtractor
import com.vibi.shared.data.repository.AndroidGallerySaver
import com.vibi.shared.data.repository.AndroidImageMetadataExtractor
import com.vibi.shared.data.repository.AndroidShareSheetLauncher
import com.vibi.shared.data.repository.AndroidVideoMetadataExtractor
import android.content.Context
import com.vibi.shared.platform.AndroidAppleSignInClient
import com.vibi.shared.platform.AndroidGoogleSignInClient
import com.vibi.shared.platform.AndroidVideoThumbnailExtractor
import com.vibi.shared.platform.AppleSignInClient
import com.vibi.shared.platform.GoogleSignInClient
import com.vibi.shared.platform.VideoThumbnailExtractor
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.vibi.shared.data.repository.AutoDubRepositoryImpl
import com.vibi.shared.data.repository.AutoSubtitleRepositoryImpl
import com.vibi.shared.data.repository.MediaJobUploader
import com.vibi.shared.domain.repository.AutoDubRepository
import com.vibi.shared.domain.repository.AutoSubtitleRepository
import com.vibi.shared.domain.usecase.input.AudioMetadataExtractor
import com.vibi.shared.domain.usecase.input.ImageMetadataExtractor
import com.vibi.shared.domain.usecase.input.VideoMetadataExtractor
import com.vibi.shared.domain.usecase.share.GallerySaver
import com.vibi.shared.domain.usecase.share.ShareSheetLauncher
import com.vibi.shared.ui.export.AndroidExportPlatformAdapter
import com.vibi.shared.ui.export.ExportPlatformAdapter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidPlatformModule = module {
    single<ExportPlatformAdapter> {
        AndroidExportPlatformAdapter(
            context = androidContext(),
            exportWithDubbing = get()
        )
    }
    single<VideoMetadataExtractor> { AndroidVideoMetadataExtractor(androidContext()) }
    single<VideoThumbnailExtractor> { AndroidVideoThumbnailExtractor(androidContext()) }
    single<AudioMetadataExtractor> { AndroidAudioMetadataExtractor(androidContext()) }
    single<ImageMetadataExtractor> { AndroidImageMetadataExtractor(androidContext()) }
    single<GallerySaver> { AndroidGallerySaver(androidContext()) }
    single<ShareSheetLauncher> { AndroidShareSheetLauncher(androidContext()) }

    single { MediaJobUploader(androidContext()) }
    single<AutoDubRepository> { AutoDubRepositoryImpl(api = get(), uploader = get(), context = androidContext()) }
    single<AutoSubtitleRepository> { AutoSubtitleRepositoryImpl(api = get(), uploader = get()) }

    // 인증 — Android 측 본 구현은 후속 phase. 현재는 stub.
    single<Settings> {
        val prefs = androidContext().getSharedPreferences("vibi_auth", Context.MODE_PRIVATE)
        SharedPreferencesSettings(prefs)
    }
    single<GoogleSignInClient> { AndroidGoogleSignInClient() }
    single<AppleSignInClient> { AndroidAppleSignInClient() }
}
