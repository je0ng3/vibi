package com.vibi.cmp

import android.app.Application
import com.vibi.shared.data.local.db.VibiDatabaseInitializer
import com.vibi.shared.di.androidPlatformModule
import com.vibi.cmp.platform.RuntimeFlags
import com.vibi.cmp.platform.sweepOrphanPickerMedia
import com.vibi.shared.di.initKoin
import com.vibi.shared.platform.ActivityProvider
import com.vibi.shared.platform.AndroidIapReconciler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class VibiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        VibiDatabaseInitializer.init(this)
        initKoin(
            bffBaseUrl = BuildConfig.BFF_BASE_URL,
            platformModules = listOf(androidPlatformModule)
        ) {
            androidLogger(Level.INFO)
            androidContext(this@VibiApplication)
        }
        // Play Billing launchBillingFlow 가 Activity 필수 — ActivityLifecycleCallbacks 로 추적.
        get<ActivityProvider>().attachTo(this)
        // 미완료 purchase (직전 세션의 BFF 가산 실패 / Ask-to-Buy 승인 등) BFF 재제출.
        // iapEnabled=false (무료 선출시) 면 구매 진입점 자체가 없어 미완료 purchase 도 없으므로
        // BillingClient.startConnection 까지 띄우는 reconciler 를 건너뛴다.
        if (RuntimeFlags.iapEnabled) {
            get<AndroidIapReconciler>().start()
        }
        // 삭제/만료 드래프트가 남긴 picker_media 영상 복사본 정리 — cascadeDeleteProject 가 sourceUri
        // 파일을 지우지 않아 filesDir 가 누적된다(iOS 도 leak 하나 Android 는 pick 마다 새 uuid 라 더 빠름).
        // 라이브 segment/bgm sourceUri 와 5분 grace 로 진행 중 pick 을 보호하며 고아 dir 만 삭제.
        CoroutineScope(Dispatchers.IO).launch { sweepOrphanPickerMedia() }
    }
}
