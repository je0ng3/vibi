package com.vibi.shared.data.repository

import kotlinx.serialization.json.Json

/**
 * Room 컬럼에 직렬화해 저장하는 JSON 값들의 공용 설정.
 * - ignoreUnknownKeys: 스키마가 진화해도 옛 row 디코드가 깨지지 않음.
 * - encodeDefaults: 기본값도 명시 저장해 read-back 일관성 유지.
 */
internal val persistedJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
