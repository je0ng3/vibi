package com.vibi.shared.platform

/**
 * Android 는 Apple native SDK 가 없어 Custom Tabs + 웹 OAuth redirect 가 필요한데, v1 은
 * iOS 우선이라 stub. App Store 정책상 iOS 에서만 Apple Sign In 이 필수이고 Android 는
 * 의무가 아니므로 후속 PR 로 분리.
 */
class AndroidAppleSignInClient : AppleSignInClient {
    override suspend fun signIn(): Result<ApplePayload> =
        Result.failure(NotImplementedError("Android Apple Sign-In not yet implemented"))

    override suspend fun signOut() {
        // no-op
    }
}
