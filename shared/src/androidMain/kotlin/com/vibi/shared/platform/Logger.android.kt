package com.vibi.shared.platform

import android.util.Log

actual fun logError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
}

actual fun logWarn(tag: String, message: String) {
    Log.w(tag, message)
}

actual fun logInfo(tag: String, message: String) {
    Log.i(tag, message)
}
