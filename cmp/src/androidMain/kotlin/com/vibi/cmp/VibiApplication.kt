package com.vibi.cmp

import android.app.Application
import com.vibi.shared.data.local.db.VibiDatabaseInitializer
import com.vibi.shared.di.androidPlatformModule
import com.vibi.shared.di.initKoin
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
    }
}
