

package com.zobaer53.zedmovies.data.network.util

import com.zobaer53.zedmovies.data.network.api.requireApiKey
import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * The TMDB client: every request resolves against [Constants.API_URL] and carries the API key, as
 * the former OkHttp interceptor added it. Tests pass a `MockEngine`; the OkHttp engine keeps
 * today's connection behavior and moves to androidMain in Phase 3.
 */
internal fun zedMoviesHttpClient(
    apiKeyProvider: zedMoviesApiKeyProvider,
    engine: HttpClientEngine = OkHttp.create(),
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
