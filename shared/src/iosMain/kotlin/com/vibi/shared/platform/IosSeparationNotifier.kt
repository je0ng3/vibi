@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.vibi.shared.platform

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS 로컬 알림. `UNUserNotificationCenter` 에 trigger=null 요청을 등록해 즉시 게시한다.
 *
 * 포그라운드 suppress 는 의도된 동작 — `willPresentNotification` delegate 를 설정하지 않으므로 앱이
 * 떠 있을 땐 iOS 가 배너를 보류한다. 사용자가 분리 진행 중 앱을 벗어났을 때만 알림이 뜬다.
 */
class IosSeparationNotifier : SeparationNotifier {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override fun requestPermission() {
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { _, _ -> }
    }

    override fun post(id: String, title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
        }
        // trigger=null → 즉시 게시. 동일 identifier 면 OS 가 기존 알림을 갱신.
        val request = UNNotificationRequest.requestWithIdentifier(id, content, null)
        center.addNotificationRequest(request, null)
    }
}
