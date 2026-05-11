package com.vibi.shared.data.local.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun createVibiDatabase(): VibiDatabase =
    getDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        // 시연 정책상 destructive migration 허용 — schema 변경 시 기존 row 는 drop.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
