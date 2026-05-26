package com.vibi.shared.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient

/**
 * @param tokenProvider 매 요청 시 호출되어 Authorization 헤더에 박을 JWT 를 반환. null 이면 헤더 생략.
 *   v1 은 보호된 라우트가 없어 effectively no-op 이지만 향후 확장 대비 hook 을 미리 박아둠.
 */
fun createBffHttpClient(
    baseUrl: String,
    // Ktor INFO 가 매 요청마다 [Ktor] METHOD URL 한 줄 + 헤더 다수 → 시뮬레이터 콘솔 도배.
    // dev 시 트래픽 검증 필요한 호출자만 명시적으로 true.
    enableLogging: Boolean = false,
    tokenProvider: () -> String? = { null },
): HttpClient =
    createPlatformHttpClient {
        expectSuccess = true

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                }
            )
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 300_000L
            // connect 는 TCP handshake 라 정상 환경이면 ms 단위. dev 시 BFF_BASE_URL 이
            // stale IP 라 닿지 않는 케이스를 5초 안에 명확히 깨도록 짧게 잡는다.
            connectTimeoutMillis = 5_000L
            socketTimeoutMillis = 300_000L
        }

        if (enableLogging) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("[Ktor] $message")
                    }
                }
                level = LogLevel.INFO
            }
        }

        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
            tokenProvider()?.let { jwt ->
                header(HttpHeaders.Authorization, "Bearer $jwt")
            }
        }
    }

/**
 * R2 presigned PUT 전용 client — baseUrl/auth/contentType default 없음. presigned URL 의
 * SigV4 는 query string 기반이라 Authorization 헤더가 있으면 R2 가 401, contentType 도
 * caller 가 sign 시점 값과 정확히 매치해야 하므로 default 박지 않음.
 *
 * [createBffHttpClient] 의 `defaultRequest { url(baseUrl) }` 와 분리해서 baseUrl 이 absolute
 * R2 URL 을 override 하는 ktor 동작 위험을 회피.
 */
fun createR2HttpClient(): HttpClient =
    createPlatformHttpClient {
        expectSuccess = true
        install(HttpTimeout) {
            // 대용량 영상 PUT — 100MB 영상 5 Mbps 업로드면 160s, margin 포함 600s 잡음.
            requestTimeoutMillis = 600_000L
            connectTimeoutMillis = 10_000L
            socketTimeoutMillis = 600_000L
        }
    }
