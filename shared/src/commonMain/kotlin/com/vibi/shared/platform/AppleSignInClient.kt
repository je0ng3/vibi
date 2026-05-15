package com.vibi.shared.platform

/**
 * Apple Sign-In 추상화. iOS 는 AuthenticationServices.framework (Swift bridge), Android 는
 * 별도 phase (Apple native SDK 없음 — Custom Tabs + 웹 OAuth 가 필요). ID Token + 최초-1회
 * fullName 만 받으면 충분 — 실제 JWKS 검증과 access token 발급은 BFF 가 담당.
 */
interface AppleSignInClient {
    /**
     * 성공 시 Apple ID Token (JWT 문자열) + fullName (최초 1회만 non-null).
     * 사용자 취소 / 실패 시 [Result.failure].
     */
    suspend fun signIn(): Result<ApplePayload>

    /**
     * Apple 은 별도 native session 이 없어 no-op 인 게 일반적. 일관성 위해 인터페이스 유지.
     */
    suspend fun signOut()
}

/**
 * Apple 인증 결과. [idToken] 만 BFF 가 검증에 사용; [fullName] 은 최초-1회 인증 시에만
 * Apple 이 제공하므로 두 번째 로그인부터 null. BFF 는 신규 가입 시에만 user.name 으로 사용.
 */
data class ApplePayload(
    val idToken: String,
    val fullName: String?,
)

/**
 * Swift 가 구현해 K/N 으로 주입하는 callback 기반 bridge.
 *
 * suspend 함수를 Swift 가 직접 구현하기 어려워 (Kotlin → Swift async 는 자동이지만 반대
 * 방향은 wrapper 필요) callback 으로 단순화. iosMain 의 [com.vibi.shared.platform.IosAppleSignInClient]
 * 가 callback 을 [kotlinx.coroutines.suspendCancellableCoroutine] 으로 suspend 화.
 */
interface AppleSignInBridge {
    fun signIn(callback: (idToken: String?, fullName: String?, errorMessage: String?) -> Unit)
}
