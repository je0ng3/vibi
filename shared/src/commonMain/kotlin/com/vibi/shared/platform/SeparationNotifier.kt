package com.vibi.shared.platform

/**
 * 음원분리 완료/실패 시 디바이스(로컬) 알림 게시. 갤러리에서 영상을 고르면 분리가 백그라운드로
 * 돌기 때문에, 사용자가 앱을 벗어난 사이 끝나면 디바이스 알림으로 알린다.
 *
 * - 앱이 **포그라운드**면 OS 가 배너를 표시하지 않는다(iOS 기본 동작 — willPresent delegate 미설정).
 *   화면 UI 가 이미 "이어서 작업" 카드로 바뀌므로 중복 알림을 띄우지 않기 위함.
 * - iOS = `UNUserNotificationCenter`. Android = no-op — 분리 자동실행이 iOS 한정
 *   ([AudioExtractor.isSupported]=false) 이라 현재 Android 엔 트리거 지점이 없다.
 */
interface SeparationNotifier {
    /** 분리 시작 시 호출 — 알림 권한을 미리 요청해 완료 시점엔 허용 상태가 되도록. 멱등. */
    fun requestPermission()

    /** 로컬 알림 즉시 게시. 동일 [id] 면 OS 가 갱신 → 중복 누적 방지. */
    fun post(id: String, title: String, body: String)
}

/** 분리 알림 문구·식별자 단일 출처. 제목 한 줄만 노출(본문 없음). */
object SeparationNotice {
    const val COMPLETE_ID = "separation_complete"
    const val COMPLETE_TITLE = "Video ready"
    const val COMPLETE_BODY = ""

    const val FAILED_ID = "separation_failed"
    const val FAILED_TITLE = "Video preparation failed"
    const val FAILED_BODY = ""
}
