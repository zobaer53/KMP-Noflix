

package com.zobaer53.zedmovies.data.network.util

import com.zobaer53.zedmovies.data.network.api.requireApiKey
import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * The TMDB client: every request resolves against [Constants.API_URL] and carries the API key, as
 * the former OkHttp interceptor added it. Tests pass a `MockEngine`; otherwise each platform
 * supplies its engine ([httpEngine]: OkHttp on Android and the JVM, as before the move).
 */
internal fun zedMoviesHttpClient(
    apiKeyProvider: zedMoviesApiKeyProvider,
    engine: HttpClientEngine = httpEngine(),
    json: Json = defaultJson
): HttpClient = HttpClient(engine) {
    // 4xx/5xx become ZedMoviesResult.Failure.HttpError in safeGet instead of exceptions.
    expectSuccess = false
    install(ContentNegotiation) { json(json) }
    defaultRequest {
        url(Constants.API_URL)
        url.parameters.append(Constants.API_KEY_QUERY_PARAM, apiKeyProvider.requireApiKey())
    }
}

/** The platform's HTTP engine (OkHttp on Android and the JVM). */
internal expect fun httpEngine(): HttpClientEngine
