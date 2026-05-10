package com.vibi.shared.di

import com.vibi.shared.data.local.AuthTokenStore
import com.vibi.shared.data.remote.api.BffApi
import com.vibi.shared.data.remote.createBffHttpClient
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val BffBaseUrlKey = named("bffBaseUrl")

val networkModule = module {
    single<HttpClient> {
        val baseUrl = getProperty<String>("bffBaseUrl")
        val tokenStore = get<AuthTokenStore>()
        createBffHttpClient(
            baseUrl = baseUrl,
            tokenProvider = { tokenStore.getValidToken() },
        )
    }
    single { BffApi(client = get()) }
}
