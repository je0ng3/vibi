package com.vibi.shared.di

import com.vibi.shared.data.local.AuthTokenStore
import com.vibi.shared.data.remote.api.BffApi
import com.vibi.shared.data.remote.createBffHttpClient
import com.vibi.shared.data.remote.createR2HttpClient
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val BffBaseUrlKey = named("bffBaseUrl")
private val R2ClientKey = named("r2Client")

val networkModule = module {
    single<HttpClient> {
        val baseUrl = getProperty<String>("bffBaseUrl")
        val tokenStore = get<AuthTokenStore>()
        createBffHttpClient(
            baseUrl = baseUrl,
            tokenProvider = { tokenStore.getValidToken() },
        )
    }
    // R2 presigned PUT 전용 — auth/baseUrl default 없음. BFF client 와 graph 상 분리.
    single<HttpClient>(R2ClientKey) { createR2HttpClient() }
    single { BffApi(client = get(), r2Client = get(R2ClientKey)) }
}
