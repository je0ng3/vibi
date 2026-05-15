package com.vibi.shared.platform

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class IosAppleSignInClient(
    private val bridge: AppleSignInBridge,
) : AppleSignInClient {

    override suspend fun signIn(): Result<ApplePayload> = suspendCancellableCoroutine { cont ->
        bridge.signIn { idToken, fullName, errorMessage ->
            val result = if (idToken != null) {
                Result.success(ApplePayload(idToken = idToken, fullName = fullName))
            } else {
                Result.failure(RuntimeException(errorMessage ?: "apple_sign_in_failed"))
            }
            if (cont.isActive) cont.resume(result)
        }
    }

    override suspend fun signOut() {
        // Apple Sign In 은 native session 이 없어 별도 cleanup 불필요.
    }
}
