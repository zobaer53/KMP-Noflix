package com.zobaer53.zedmovies.data.network.util

import com.zobaer53.zedmovies.data.common.result.HttpException
import com.zobaer53.zedmovies.data.common.result.ZedMoviesResult
import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeRequestTest {
    @Serializable
    private data class Page(val page: Int)

    private val apiKey = object : zedMoviesApiKeyProvider {
        override val apiKey = "secret"
    }

    private fun client(handler: MockRequestHandler) = zedMoviesHttpClient(apiKey, MockEngine(handler))

    private fun json(body: String, status: HttpStatusCode = HttpStatusCode.OK): MockRequestHandler = {
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    @Test
    fun `2xx with a body is a success with status and url`() = runTest {
        val result = client(json("""{"page":3}""")).safeGet<Page>("movie/popular") { parameter("page", 3) }
        val success = result as ZedMoviesResult.Success.HttpResponse
        assertEquals(Page(3), success.value)
        assertEquals(200, success.statusCode)
        assertEquals("OK", success.statusMessage)
        assertEquals("https://api.themoviedb.org/3/movie/popular?page=3", success.url)
    }

    @Test
    fun `the api key is sent but never reported`() = runTest {
        var sentKey: String? = null
        val result = client {
            sentKey = it.url.parameters["api_key"]
            respond("""{"page":1}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }.safeGet<Page>("movie/popular")
        assertEquals("secret", sentKey)
        assertFalse((result as ZedMoviesResult.Success.HttpResponse).url!!.contains("secret"))
        assertFalse(result.url!!.contains("api_key"))
    }

    @Test
    fun `2xx with bad JSON is an error`() = runTest {
        val result = client(json("""{"page":"three"""")).safeGet<Page>("movie/popular")
        assertTrue((result as ZedMoviesResult.Failure.Error).error is SerializationException)
    }

    @Test
    fun `4xx is an HttpError with status and url`() = runTest {
        val unauthorized = client { respondError(HttpStatusCode.Unauthorized) }.safeGet<Page>("movie/popular")
        val error = (unauthorized as ZedMoviesResult.Failure.HttpError).error
        assertEquals(401, error.statusCode)
        assertEquals("Unauthorized", error.statusMessage)
        assertEquals("https://api.themoviedb.org/3/movie/popular", error.url)

        val notFound = client { respondError(HttpStatusCode.NotFound) }.safeGet<Page>("movie/0")
        assertEquals(404, (notFound as ZedMoviesResult.Failure.HttpError).statusCode)
        assertTrue(notFound.error is HttpException)
    }

    @Test
    fun `no network is an error with the IOException`() = runTest {
        // Coroutine stack-trace recovery may hand back a copy, so compare type and message, not identity.
        val result = client { throw IOException("offline") }.safeGet<Page>("movie/popular")
        val error = (result as ZedMoviesResult.Failure.Error).error
        assertTrue(error is IOException)
        assertEquals("offline", error.message)
    }

    @Test
    fun `cancellation is rethrown`() = runTest {
        val thrown = runCatching { client { throw CancellationException("cancelled") }.safeGet<Page>("movie/popular") }
            .exceptionOrNull()
        assertTrue(thrown is CancellationException)
    }
}
