package com.vibi.shared.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Android 로컬 알림. iOS [IosSeparationNotifier] 동등물 — `NotificationManagerCompat` 로 즉시 게시한다.
 *
 * - init 에서 [CHANNEL_ID] 채널(IMPORTANCE_DEFAULT)을 API26+ 에 1회 생성한다(pre-O 는 채널 개념이
 *   없어 no-op).
 * - [post] 는 [NotificationCompat.Builder] 로 알림을 만들어 [SeparationNotice] 의 문자열 id 를 고정
 *   int 로 매핑해 게시한다 → 동일 종류 재게시 시 누적 대신 갱신(iOS 의 동일 identifier 갱신과 동등).
 *   API33+ 에선 `POST_NOTIFICATIONS` 미허용 시 조용히 skip 한다(권한 미부여 시 notify 가 무시됨).
 * - [requestPermission] 은 API33+ 에서 미허용 시 현재 Activity 로 권한 요청을 fire-and-forget 한다.
 *   결과 콜백은 받지 않는다 — iOS 의 `requestAuthorizationWithOptions { _, _ -> }` 와 동일하게
 *   "완료 시점에 허용 상태가 되도록 미리 한 번 요청" 하는 멱등 호출이다. API32 이하는 no-op
 *   (런타임 알림 권한 자체가 없어 설치 시 부여됨).
 */
class AndroidSeparationNotifier(
    private val context: Context,
    private val activityProvider: ActivityProvider,
) : SeparationNotifier {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }

    override fun requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (hasPostPermission()) return
        // 결과는 받지 않는다(멱등 사전 요청). 띄울 Activity 가 없으면 조용히 skip.
        activityProvider.current?.let { activity ->
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                PERMISSION_REQUEST_CODE,
            )
        }
    }

    override fun post(id: String, title: String, body: String) {
        // API33+ 에서 권한 미부여면 notify 가 무시되므로 미리 차단(불필요한 호출 방지).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostPermission()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(stableIntId(id), notification)
    }

    private fun hasPostPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * [SeparationNotice] 의 문자열 id 를 고정 int 로 매핑 → 동일 종류 알림이 누적 없이 갱신되도록.
     * (iOS 는 동일 identifier 면 OS 가 갱신하지만, Android `notify` 는 int id 기준이라 매핑 필요.)
     */
    private fun stableIntId(id: String): Int = when (id) {
        SeparationNotice.COMPLETE_ID -> NOTIFY_ID_COMPLETE
        SeparationNotice.FAILED_ID -> NOTIFY_ID_FAILED
        else -> id.hashCode()
    }

    private companion object {
        const val CHANNEL_ID = "separation"
        const val CHANNEL_NAME = "Audio separation"
        const val PERMISSION_REQUEST_CODE = 0

        const val NOTIFY_ID_COMPLETE = 1
        const val NOTIFY_ID_FAILED = 2
    }
}
