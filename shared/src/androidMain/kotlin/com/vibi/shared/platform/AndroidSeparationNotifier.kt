package com.vibi.shared.platform

/**
 * Android no-op stub. 음원분리 자동실행이 iOS 한정([AudioExtractor.isSupported]=false) 이라 현재
 * Android 엔 분리 완료/실패 알림을 띄울 트리거 지점이 없다. Android 분리 활성화 시
 * `NotificationManagerCompat` + `POST_NOTIFICATIONS` 권한으로 본 구현.
 * ([AndroidAppleSignInClient] 등과 동일한 "후속 phase stub" 정책.)
 */
class AndroidSeparationNotifier : SeparationNotifier {
    override fun requestPermission() = Unit
    override fun post(id: String, title: String, body: String) = Unit
}
