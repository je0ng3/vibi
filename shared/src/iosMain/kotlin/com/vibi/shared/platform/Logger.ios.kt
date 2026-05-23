package com.vibi.shared.platform

import platform.Foundation.NSLog

actual fun logError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        NSLog("[E][$tag] $message — ${throwable::class.simpleName}: ${throwable.message ?: ""}\n${throwable.stackTraceToString()}")
    } else {
        NSLog("[E][$tag] $message")
    }
}

actual fun logWarn(tag: String, message: String) {
    NSLog("[W][$tag] $message")
}

actual fun logInfo(tag: String, message: String) {
    NSLog("[I][$tag] $message")
}
