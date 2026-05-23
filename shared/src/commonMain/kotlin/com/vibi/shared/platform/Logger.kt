package com.vibi.shared.platform

/**
 * KMP 공통 로깅 진입점. 사용자 메시지(snackbar/toast 등)와 디버그 로그를 분리하기 위한 contract.
 *
 * - `logError(tag, message, throwable)` — 운영 디버깅용. tag 로 grep, throwable.stackTrace 보존.
 * - `logWarn` / `logInfo` — 정보성 (release 빌드에서 자동 down-level 은 platform actual 책임).
 *
 * release 빌드 verbose suppress 는 platform actual 에서 BuildConfig.DEBUG / Kotlin/Native flag 로 처리.
 */
expect fun logError(tag: String, message: String, throwable: Throwable? = null)
expect fun logWarn(tag: String, message: String)
expect fun logInfo(tag: String, message: String)
