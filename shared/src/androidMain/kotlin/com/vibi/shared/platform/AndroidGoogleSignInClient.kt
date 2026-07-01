package com.vibi.shared.platform

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Android Google 로그인 — Credential Manager + "Sign in with Google".
 *
 * [GoogleSignInClient] 계약대로 성공 시 Google ID Token(JWT) 을 돌려준다. 그 idToken 의
 * aud 는 [webClientId] (WEB/server OAuth client id) 이며, BFF GOOGLE_OAUTH_CLIENT_IDS 에
 * 이 값이 포함돼 있어야 /auth/google 검증을 통과한다. (iOS 는 iOS client id 를 쓴다 — 플랫폼별
 * client id 가 다른 건 Google 정책. 같은 GCP 프로젝트면 BFF 에 WEB client id 한 줄 추가로 양쪽 수용.)
 *
 * 절대 throw 하지 않는다 — 취소/실패는 모두 [Result.failure] 로 매핑한다. AuthRepository 가
 * getOrThrow 로 풀어 LoginViewModel 에서 "Sign-in failed" 로 표시.
 */
class AndroidGoogleSignInClient(
    private val appContext: Context,
    private val activityProvider: ActivityProvider,
    private val webClientId: String,
) : GoogleSignInClient {

    private val credentialManager by lazy { CredentialManager.create(appContext) }

    override suspend fun signIn(): Result<String> {
        // Credential Manager 의 계정 선택 시트는 Activity context 를 요구 (Application context 불가).
        val activity = activityProvider.current
            ?: return Result.failure(IllegalStateException("no_presenting_activity"))
        if (webClientId.isBlank()) {
            return Result.failure(IllegalStateException("missing_google_web_client_id"))
        }
        return try {
            // 명시적 "Sign in with Google" 버튼이므로 항상 계정 선택을 띄운다 (iOS GIDSignIn 동등).
            val option = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val response = credentialManager.getCredential(activity, request)
            extractIdToken(response.credential)
        } catch (e: GetCredentialException) {
            // 사용자 취소(GetCredentialCancellationException) 포함 — 전부 failure 로.
            Result.failure(RuntimeException(e.message ?: "google_sign_in_failed", e))
        }
    }

    override suspend fun signOut() {
        // 로컬 credential 상태만 정리. 토큰 저장소 삭제는 AuthRepository 가 별도 처리.
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    private fun extractIdToken(credential: Credential): Result<String> =
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            runCatching { GoogleIdTokenCredential.createFrom(credential.data).idToken }
                .fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { Result.failure(RuntimeException("missing_id_token", it)) },
                )
        } else {
            Result.failure(RuntimeException("unexpected_credential_type"))
        }
}
