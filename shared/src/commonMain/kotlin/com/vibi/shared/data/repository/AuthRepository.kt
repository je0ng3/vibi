package com.vibi.shared.data.repository

import com.vibi.shared.data.local.AuthTokenStore
import com.vibi.shared.data.local.UserSession
import com.vibi.shared.data.local.extractJwtSubject
import com.vibi.shared.data.remote.api.BffApi
import com.vibi.shared.domain.model.AuthUser
import com.vibi.shared.platform.GoogleSignInClient

/**
 * Google OAuth → BFF JWT 교환 + 로컬 토큰 캐시 + 계정별 로컬 데이터 스코핑.
 *
 * - [signInWithGoogle] — native SDK 로 Google ID Token → BFF /auth/google → JWT 저장 + [UserSession] 갱신.
 *   row 자체는 userId 컬럼으로 scoped — 다른 계정 row 를 wipe 하지 않고도 UI query
 *   (`observeAllForUser`) 에서 격리되므로 A↔B 왕복 시 각자 작업 누적 유지.
 * - [hasValidSession] — 저장된 JWT 가 만료 안 됐는지 (스플래시에서 라우팅 결정용).
 * - [restoreSession] — 앱 시작 시 저장된 JWT 의 sub 를 [UserSession] 으로 복원.
 * - [signOut] — Google 세션 + 토큰 캐시 정리. 로컬 데이터는 보존.
 */
class AuthRepository(
    private val signInClient: GoogleSignInClient,
    private val bffApi: BffApi,
    private val tokenStore: AuthTokenStore,
    private val userSession: UserSession,
) {
    suspend fun signInWithGoogle(): Result<AuthUser> = runCatching {
        val idToken = signInClient.signIn().getOrThrow()
        val resp = bffApi.exchangeGoogleIdToken(idToken)
        tokenStore.saveToken(resp.accessToken, resp.expiresAt)
        val newUserId = extractJwtSubject(resp.accessToken) ?: resp.user.sub
        switchUser(newUserId)
        resp.user.toDomain()
    }

    fun hasValidSession(): Boolean = tokenStore.getValidToken() != null

    /** 앱 시작 시 토큰의 sub 또는 lastUserId 로 [UserSession] 복원. */
    fun restoreSession() {
        val sub = tokenStore.getValidToken()?.let(::extractJwtSubject)
        val resolved = sub ?: tokenStore.lastUserId() ?: UserSession.ANONYMOUS_USER_ID
        userSession.set(resolved)
    }

    suspend fun signOut() {
        // 로컬 row 는 보존 — 같은 계정 재로그인 시 "이어서 작업" 복원.
        // 다른 계정 세션에서는 row 의 userId 컬럼 매칭으로 자동 격리되므로 wipe 불필요.
        // 토큰 먼저 삭제 — native SDK signOut 이 throw 해도 로컬 세션은 끊긴 상태로 남도록.
        tokenStore.clear()
        userSession.reset()
        runCatching { signInClient.signOut() }
    }

    private fun switchUser(newUserId: String) {
        // wipe 없음 — A→B→A 왕복 시 각자 작업 누적 유지. UI 격리는 user-scoped query 가 처리.
        tokenStore.saveLastUserId(newUserId)
        userSession.set(newUserId)
    }
}
