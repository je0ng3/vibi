package com.vibi.shared.data.local.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun createVibiDatabase(): VibiDatabase =
    getDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        // v1 baseline — 기존 사용자 DB 가 v32 schema 라면 이 옵션이 drop+recreate 한다.
        // v1 시연 정책상 destructive 허용.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
