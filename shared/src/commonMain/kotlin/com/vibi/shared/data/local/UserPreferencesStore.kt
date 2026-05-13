package com.vibi.shared.data.local

import com.russhwolf.settings.Settings

/**
 * 사용자 UI 선호도 (Don't-ask-again 류) 저장. NSUserDefaults / SharedPreferences 평문.
 *
 * - [localizationLockSuppressed]: 음원 → 자막/더빙 탭 전환 시 "음원분리 수정 불가" 경고를 다시 띄울지.
 * - [editResetSuppressed]: 영상편집 탭 전환 시 "기존 음원/자막/더빙 초기화" 경고를 다시 띄울지.
 */
class UserPreferencesStore(private val settings: Settings) {

    var localizationLockSuppressed: Boolean
        get() = settings.getBoolean(KEY_LOCALIZATION_LOCK, false)
        set(value) { settings.putBoolean(KEY_LOCALIZATION_LOCK, value) }

    var editResetSuppressed: Boolean
        get() = settings.getBoolean(KEY_EDIT_RESET, false)
        set(value) { settings.putBoolean(KEY_EDIT_RESET, value) }

    companion object {
        private const val KEY_LOCALIZATION_LOCK = "warn.audio_to_localization.suppressed"
        private const val KEY_EDIT_RESET = "warn.back_to_edit.suppressed"
    }
}
