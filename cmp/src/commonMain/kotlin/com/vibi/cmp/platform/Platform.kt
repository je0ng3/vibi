package com.vibi.cmp.platform

/**
 * 현재 실행 플랫폼이 iOS 인지. Apple Sign In 처럼 iOS 한정으로만 노출해야 하는 UI 게이팅용.
 * Android 에는 Apple 네이티브 로그인이 없어 LoginScreen 의 Apple 버튼을 숨긴다.
 */
expect val isIosPlatform: Boolean
