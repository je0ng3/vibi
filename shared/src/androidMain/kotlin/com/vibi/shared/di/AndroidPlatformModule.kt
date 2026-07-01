package com.vibi.shared.di

import com.vibi.shared.BuildConfig
import com.vibi.shared.data.repository.AndroidAudioMetadataExtractor
import com.vibi.shared.data.repository.AndroidGallerySaver
import com.vibi.shared.data.repository.AndroidShareSheetLauncher
import com.vibi.shared.data.repository.AndroidVideoMetadataExtractor
import com.vibi.shared.data.repository.RemoteRenderExecutor
import com.vibi.shared.domain.usecase.export.FfmpegExecutor
import android.content.Context
import com.vibi.shared.platform.ActivityProvider
import com.vibi.shared.platform.AndroidAppleSignInClient
import com.vibi.shared.platform.AndroidGoogleSignInClient
import com.vibi.shared.platform.AndroidAudioExtractor
import com.vibi.shared.platform.AndroidSeparationNotifier
import com.vibi.shared.platform.SeparationNotifier
import com.vibi.shared.platform.AndroidIapClient
import com.vibi.shared.platform.AndroidIapReconciler
import com.vibi.shared.platform.AndroidVideoThumbnailExtractor
import com.vibi.shared.platform.AppleSignInClient
import com.vibi.shared.platform.AudioExtractor
import com.vibi.shared.platform.GoogleSignInClient
import com.vibi.shared.platform.VideoThumbnailExtractor
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.vibi.shared.domain.usecase.input.AudioMetadataExtractor
import com.vibi.shared.domain.usecase.input.VideoMetadataExtractor
import com.vibi.shared.domain.usecase.share.GallerySaver
import com.vibi.shared.domain.usecase.share.ShareSheetLauncher
import com.vibi.shared.ui.export.AndroidExportPlatformAdapter
import com.vibi.shared.ui.export.ExportPlatformAdapter
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val androidPlatformModule = module {
    // v2 multipart render path 유지 — v3 (V3RenderExecutor) 는 sha256/statFile actual 이
    // Android 에 없어 런타임 throw. iOS 가 v3 안정화 완료 후 별도 phase 에서 Android v3 작업.
    single<FfmpegExecutor> { get<RemoteRenderExecutor>() }
    single<ExportPlatformAdapter> {
        AndroidExportPlatformAdapter(executor = get())
    }
    single<VideoMetadataExtractor> { AndroidVideoMetadataExtractor(androidContext()) }
    single<VideoThumbnailExtractor> { AndroidVideoThumbnailExtractor(androidContext()) }
    single<AudioMetadataExtractor> { AndroidAudioMetadataExtractor(androidContext()) }
    single<AudioExtractor> { AndroidAudioExtractor() }
    single<SeparationNotifier> { AndroidSeparationNotifier(androidContext(), get()) }
    single<GallerySaver> { AndroidGallerySaver(androidContext()) }
    single<ShareSheetLauncher> { AndroidShareSheetLauncher(androidContext(), get()) }

    // 인증 — Android 측 본 구현은 후속 phase. 현재는 stub.
    single<Settings> {
        val prefs = androidContext().getSharedPreferences("vibi_auth", Context.MODE_PRIVATE)
        SharedPreferencesSettings(prefs)
    }
    // 토큰 전용 보안 저장소. EncryptedSharedPreferences(security-crypto)는 deprecated 라 도입
    // 보류하고, manifest allowBackup=false 로 토큰의 백업 유출 벡터를 차단한 기존 prefs 를 재사용.
    // (향후 androidx Keystore 기반으로 교체 시 이 바인딩만 바꾸면 됨.)
    single<Settings>(named(SECURE_SETTINGS)) { get() }
    single<GoogleSignInClient> {
        AndroidGoogleSignInClient(
            appContext = androidContext(),
            activityProvider = get(),
            webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
        )
    }
    single<AppleSignInClient> { AndroidAppleSignInClient() }

    // Play Billing — ActivityProvider 는 VibiApplication.onCreate 에서 attachTo() 호출.
    // AndroidIapReconciler.start() 도 같은 시점에 트리거 (KoinHelper 동등 흐름).
    single { ActivityProvider() }
    single { AndroidIapClient(appContext = androidContext(), activityProvider = get()) }
    single { AndroidIapReconciler(iap = get(), creditPurchaseService = get()) }
}
