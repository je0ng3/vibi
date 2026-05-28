package com.vibi.shared.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * 현재 foreground Activity 의 weak ref 를 보관. [AndroidIapClient.launchBillingFlow] 가
 * Activity context 를 요구하므로, KMP layer ([com.vibi.shared.platform.IapBridge] 동등물)
 * 에서 Application context 만 들고 있으면 Activity 를 얻을 수 없어 필요.
 *
 * Application 이 살아있는 동안 단일 인스턴스 — [attachTo] 를 `VibiApplication.onCreate` 에서
 * 1회만 호출. Activity 가 stop/destroy 되면 ref 를 null 로 비워 메모리 leak 방지.
 */
class ActivityProvider {
    @Volatile
    private var ref: WeakReference<Activity>? = null

    val current: Activity? get() = ref?.get()

    fun attachTo(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) {
                ref = WeakReference(activity)
            }
            override fun onActivityResumed(activity: Activity) {
                ref = WeakReference(activity)
            }
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) {
                if (ref?.get() === activity) ref = null
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) {
                if (ref?.get() === activity) ref = null
            }
        })
    }
}
