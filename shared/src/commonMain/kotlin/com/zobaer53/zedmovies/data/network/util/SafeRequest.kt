

package com.zobaer53.zedmovies.data.network.util

import com.zobaer53.zedmovies.data.common.result.HttpException
import com.zobaer53.zedmovies.data.common.result.ZedMoviesResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.request
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException

/**
 * A GET that never throws (except for cancellation), mapped like the former Retrofit call adapter:
 * 2xx → [ZedMoviesResult.Success.HttpResponse]; 4xx/5xx → [ZedMoviesResult.Failure.HttpError];
 * no network, timeouts, bad JSON, … → [ZedMoviesResult.Failure.Error] with the exception.
 * [path] is relative to [Constants.API_URL]; the reported url never contains the API key.
 */
@Suppress("TooGenericExceptionCaught")
internal suspend inline fun <reified T> HttpClient.safeGet(
    path: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): ZedMoviesResult<T> = try {
    val response = get(path) { block() }
    val url = response.request.url.withoutApiKey()
    if (response.status.isSuccess()) {
        ZedMoviesResult.success(response.body<T>(), response.status.value, response.status.description, url)
    } else {
        ZedMoviesResult.failure(
            HttpException(statusCode = response.status.value, statusMessage = response.status.description, url = url)
        )
    }
} catch (exception: CancellationException) {
    throw exception
} catch (exception: ContentConvertException) {
    // Ktor wraps bad JSON; report the SerializationException itself, as the Retrofit converter did.
    ZedMoviesResult.failure(exception.cause ?: exception)
} catch (exception: Exception) {
    ZedMoviesResult.failure(exception)
}

@PublishedApi
internal fun Url.withoutApiKey(): String =
    URLBuilder(this).apply { parameters.remove(Constants.API_KEY_QUERY_PARAM) }.buildString()
