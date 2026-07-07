package com.vibi.shared.data.local

import com.russhwolf.settings.Settings

/**
 * "동영상(draft) 삭제" 확인 다이얼로그의 **"다시 보지 않기"** 영속 플래그.
 *
 * 입력 화면 "이어서 작업" 카드의 X 삭제([com.vibi.shared.ui.input.InputViewModel.onDeleteDraft])가
 * 프로젝트와 자식 row 전부를 되돌릴 수 없이 지우므로, 기본은 확인 팝업을 띄우고
 * 사용자가 체크하면 이후 즉시 삭제한다.
 *
 * 키는 **계정별**(user-scoped): `draft.delete.skipWarning.<userId>`
 * ([SeparationCancelWarningStore] 와 동일 패턴) — 공용 기기에서 다른 계정 사용자가
 * 무경고로 삭제하는 사고를 막기 위함.
 */
class DraftDeleteWarningStore(
    private val settings: Settings,
    private val userSession: UserSession,
) {
    /** 사용자가 "다시 보지 않기" 를 체크해 확인 팝업을 건너뛰기로 했는지 (현재 로그인 계정 기준). */
    val skip: Boolean
        get() = settings.getBoolean(keyFor(userSession.current()), false)

    /** "다시 보지 않기" 토글 영속. 확인 다이얼로그의 체크박스에서 호출. */
    fun setSkip(skip: Boolean) {
        settings.putBoolean(keyFor(userSession.current()), skip)
    }

    private fun keyFor(userId: String): String = "$KEY_PREFIX.$userId"

    private companion object {
        const val KEY_PREFIX = "draft.delete.skipWarning"
    }
}
