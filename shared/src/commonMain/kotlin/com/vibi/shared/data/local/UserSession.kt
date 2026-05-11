package com.vibi.shared.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 현재 로그인된 사용자의 식별자 (JWT sub claim) 를 앱 lifetime 동안 보유.
 * 리포지토리/DAO 가 user-scoped 쿼리를 만들 때 참조한다.
 *
 * 시연용 sign-in 우회 (LoginViewModel) 경로에서는 [ANONYMOUS_USER_ID] 가 들어간다.
 * 실제 OAuth 복구 후에는 BFF JWT 의 sub 가 들어간다.
 */
class UserSession {
    private val _userId = MutableStateFlow<String>(ANONYMOUS_USER_ID)
    val userId: StateFlow<String> = _userId.asStateFlow()

    fun current(): String = _userId.value

    fun set(userId: String) {
        _userId.value = userId.ifBlank { ANONYMOUS_USER_ID }
    }

    fun reset() {
        _userId.value = ANONYMOUS_USER_ID
    }

    companion object {
        const val ANONYMOUS_USER_ID = "anonymous"
    }
}
