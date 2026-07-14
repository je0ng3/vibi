package com.vibi.shared.data.remote.dto

import com.vibi.shared.domain.model.AuthUser
import com.vibi.shared.domain.model.IdentityProvider
import com.vibi.shared.domain.model.LinkedIdentity
import kotlinx.serialization.Serializable

@Serializable
data class GoogleAuthRequestDto(
    val idToken: String,
)

/**
 * Apple Sign In ID Token 교환 요청.
 *
 * - [fullName] — Apple 은 사용자 동의 흐름에서 **최초 1회만** fullName 을 준다.
 *   iOS 가 그 시점에 받은 fullName 을 그대로 전달하고, 두 번째 로그인부터는 null.
 *   서버는 신규 가입 시에만 이 값을 user.name 으로 채우고, 이후엔 DB 의 기존 name 보존.
 */
@Serializable
data class AppleAuthRequestDto(
    val idToken: String,
    val fullName: String? = null,
)

@Serializable
data class AuthUserDto(
    val sub: String,
    val email: String,
    val name: String,
    val picture: String? = null,
    val role: String = AuthUser.ROLE_USER,
) {
    fun toDomain(): AuthUser = AuthUser(
        sub = sub,
        email = email,
        name = name,
        picture = picture,
        role = role,
    )
}

@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val expiresAt: Long,
    val user: AuthUserDto,
)

/**
 * 계정에 연결된 로그인 수단 1개 — BFF `GET /auth/identities` 항목과 1:1.
 *
 * [provider] 는 "google"/"apple" wire 값. [primary] 는 최초 가입 provider(계정당 정확히 1개).
 * 알 수 없는 provider 문자열이면 [toDomain] 이 null 을 돌려 상위에서 걸러낸다(전방 호환).
 */
@Serializable
data class LinkedIdentityDto(
    val provider: String,
    val email: String,
    val primary: Boolean,
) {
    fun toDomain(): LinkedIdentity? =
        IdentityProvider.fromWire(provider)?.let { p ->
            LinkedIdentity(provider = p, email = email, primary = primary)
        }
}

/** `GET /auth/identities` · `DELETE /auth/link/{provider}` 응답 — 연결된 identity 목록. */
@Serializable
data class IdentitiesResponseDto(
    val identities: List<LinkedIdentityDto> = emptyList(),
)

/**
 * `POST /auth/link/{provider}` 응답.
 *
 * - [status] — "linked"(신규 연결) / "already_linked"(멱등) / "merged"(다른 계정 흡수).
 * - [mergedCredits] — merged 시 이월된 크레딧(무료 보너스 제외한 결제·광고분). 그 외 null.
 * - [creditBalance] — merged 시 병합 후 현재 계정 잔액. 그 외 null.
 * - [identities] — 연결 후 전체 identity 목록(클라이언트 UI 갱신용).
 */
@Serializable
data class LinkResponseDto(
    val status: String,
    val creditBalance: Int? = null,
    val mergedCredits: Int? = null,
    val identities: List<LinkedIdentityDto> = emptyList(),
)
