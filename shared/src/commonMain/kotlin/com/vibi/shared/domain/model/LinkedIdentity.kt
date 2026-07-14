package com.vibi.shared.domain.model

/**
 * 계정에 연결할 수 있는 소셜 로그인 provider. wire/DB 값("google"/"apple")과 1:1.
 *
 * BFF 의 `AuthProvider` 와 대응하는 앱측 표현 — 링크/언링크 액션 대상 지정과 UI 라벨 선택에 쓴다.
 */
enum class IdentityProvider(val wire: String) {
    GOOGLE("google"),
    APPLE("apple"),
    ;

    companion object {
        /** wire 문자열 → enum(대소문자 무시). 미지원 값은 null — [wire] 의 역함수. */
        fun fromWire(raw: String): IdentityProvider? =
            raw.lowercase().let { v -> entries.firstOrNull { it.wire == v } }
    }
}

/**
 * 계정(users)에 연결된 로그인 수단 1개. BFF `GET /auth/identities` 항목의 도메인 표현.
 *
 * [primary] 는 최초 가입 provider(계정당 정확히 1개) — UI 가 "기본" 배지를 표시하고, 마지막 하나만
 * 남았을 때 해제 버튼을 감추는 데 쓴다(마지막 로그인 수단 해제는 BFF 가 409 로 막는다).
 */
data class LinkedIdentity(
    val provider: IdentityProvider,
    val email: String,
    val primary: Boolean,
)
