package com.vibi.shared.data.local

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * JWT payload 의 `sub` claim 만 추출. 서명 검증·exp 체크는 안 함 (별도 [AuthTokenStore.getValidToken]
 * 에서 만료 체크함). 잘못된 포맷 / 디코드 실패 시 null.
 */
@OptIn(ExperimentalEncodingApi::class)
internal fun extractJwtSubject(jwt: String): String? = runCatching {
    val parts = jwt.split('.')
    require(parts.size >= 2)
    val payloadBytes = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
        .decode(parts[1])
    val payload = Json.parseToJsonElement(payloadBytes.decodeToString()).jsonObject
    payload["sub"]?.jsonPrimitive?.contentOrNull
}.getOrNull()
