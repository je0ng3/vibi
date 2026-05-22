package com.vibi.shared.data.local

import com.russhwolf.settings.Settings

/**
 * 사용자 UI 선호도 저장. NSUserDefaults / SharedPreferences 평문.
 *
 * 자막/더빙 제거 후 활성 prefs 는 없음 — 향후 추가 위해 클래스/Settings 의존성은 유지.
 */
@Suppress("unused")
class UserPreferencesStore(private val settings: Settings)
