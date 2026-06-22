package com.vibi.shared.data.local.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun createVibiDatabase(): VibiDatabase =
    getDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        // v14 = 정식 출시 baseline. 출시 전 스키마(v1~13)에서 올라오는 기존 dev/테스터 설치만
        // 1회 destructive wipe 로 v14 로 정리하고, v14 이후(v15+) 업그레이드는 destructive 금지 —
        // Migration/AutoMigration 누락 시 런타임에서 실패해 사용자 프로젝트(영상 segment·BGM·
        // 음원분리 directive)를 보존한다. exportSchema=true + schemas/14.json 으로 향후 AutoMigration
        // 검증 가능. downgrade(스토어 외 dev 롤백)만 destructive 허용.
        .fallbackToDestructiveMigrationFrom(
            dropAllTables = true,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        )
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()
