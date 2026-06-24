package com.vibi.shared.data.local

import com.russhwolf.settings.Settings

/**
 * "음원분리 취소 시 크레딧 환불 불가" 경고 다이얼로그의 **"다시 보지 않기"** 영속 플래그.
 *
 * 진행 중 분리를 취소하는 경로가 둘 — 타임라인의 "Cancel separation"([com.vibi.shared.ui.timeline.TimelineViewModel])
 * 과 입력 화면의 "준비중" 카드 X([com.vibi.shared.ui.input.InputViewModel]) — 라서, 같은 플래그를 두 곳이
 * 공유한다. 키 문자열을 한 곳에만 두어 양쪽이 어긋나지 않도록 본 store 로 묶었다.
 *
 * 키는 **계정별**(user-scoped): `separation.cancel.skipWarning.<userId>` ([CreditStore] 와 동일 패턴).
 * 같은 기기라도 다른 계정으로 로그인하면 그 계정은 한 번도 체크한 적이 없으므로 경고를 다시 본다 —
 * 크레딧 소모 경고라 공용 기기에서 다른 사용자가 무경고로 취소(환불 불가)하는 사고를 막기 위함.
 */
class SeparationCancelWarningStore(
    private val settings: Settings,
    private val userSession: UserSession,
) {
    /** 사용자가 "다시 보지 않기" 를 체크해 경고를 건너뛰기로 했는지 (현재 로그인 계정 기준). */
    val skip: Boolean
        get() = settings.getBoolean(keyFor(userSession.current()), false)

    /** "다시 보지 않기" 토글 영속. 경고 다이얼로그의 체크박스에서 호출. */
    fun setSkip(skip: Boolean) {
        settings.putBoolean(keyFor(userSession.current()), skip)
    }

    private fun keyFor(userId: String): String = "$KEY_PREFIX.$userId"

    private companion object {
        const val KEY_PREFIX = "separation.cancel.skipWarning"
    }
}
