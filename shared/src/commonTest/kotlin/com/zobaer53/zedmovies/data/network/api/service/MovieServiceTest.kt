package com.zobaer53.zedmovies.data.network.api.service

import com.zobaer53.zedmovies.data.common.result.ZedMoviesResult
import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import com.zobaer53.zedmovies.data.network.util.zedMoviesHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class MovieServiceTest {
    private val requests = mutableListOf<Url>()
    private val service = MovieService(
        zedMoviesHttpClient(
            apiKeyProvider = object : zedMoviesApiKeyProvider {
                override val apiKey = "k"
            },
            engine = MockEngine { request ->
                requests += request.url
                respond(EMPTY_PAGE, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        )
    )

    @Test
    fun `discover sends the filters, language, page and api key`() = runTest {
        val filters = mapOf("sort_by" to "popularity.desc", "with_genres" to "28|12", "vote_average.gte" to "7.5")
        val result = service.discover(filters, language = "de", page = 2)
        assertTrue(result is ZedMoviesResult.Success)
        val url = requests.single()
        assertEquals("/3/discover/movie", url.encodedPath)
        assertEquals("popularity.desc", url.parameters["sort_by"])
        assertEquals("28|12", url.parameters["with_genres"])
        assertEquals("7.5", url.parameters["vote_average.gte"])
        assertEquals("de", url.parameters["language"])
        assertEquals("2", url.parameters["page"])
        assertEquals("k", url.parameters["api_key"])
    }

    @Test
    fun `search omits a null year and sends a set one`() = runTest {
        service.search("Batman", language = "en")
        assertNull(requests.last().parameters["primary_release_year"])
        assertEquals("Batman", requests.last().parameters["query"])
        assertEquals("1", requests.last().parameters["page"])

        service.search("Batman", language = "en", primaryReleaseYear = 2005)
        assertEquals("2005", requests.last().parameters["primary_release_year"])
    }

    @Test
    fun `details put the id in the path and append credits and videos`() = runTest {
        service.getDetailsById(550, language = "en", includeVideoLanguage = "en,null")
        val url = requests.single()
        assertEquals("/3/movie/550", url.encodedPath)
        assertEquals("credits,videos", url.parameters["append_to_response"])
        assertEquals("en,null", url.parameters["include_video_language"])
    }

    private companion object {
        const val EMPTY_PAGE = """{"page":1,"results":[],"total_pages":1,"total_results":0}"""
    }
}
