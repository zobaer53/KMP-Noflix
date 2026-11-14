package com.zobaer53.zedmovies.data.network.common

import com.zobaer53.zedmovies.data.common.result.HttpException
import com.zobaer53.zedmovies.data.common.result.ZedMoviesResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

// Pins fix F2: the isSuccess()/isFailure() contracts used to claim the type check held when they returned
// false, so after a failed isSuccess() Kotlin/Native smart-cast the response to Success and crashed on
// every network failure. A failing fetch must emit the cached value as a Failure and never throw.
class NetworkBoundResourceTest {
    @Test
    fun successSavesTheResponseAndReEmitsFromTheCache() = runTest {
        var cache = "cached"
        val results = networkBoundResource(
            query = { flowOf(cache) },
            fetch = { ZedMoviesResult.success("fresh") },
            saveFetchResult = { cache = it }
        ).toList()

        assertEquals(3, results.size)
        assertEquals(ZedMoviesResult.Loading<String>(null), results[0])
        assertEquals(ZedMoviesResult.Loading("cached"), results[1])
        assertIs<ZedMoviesResult.Success<String>>(results[2])
        assertEquals("fresh", (results[2] as ZedMoviesResult.Success<String>).value)
    }

    @Test
    fun failedFetchEmitsTheCachedValueAsAFailure() = runTest {
        val error = IllegalStateException("offline")
        val results = networkBoundResource<String, String>(
            query = { flowOf("cached") },
            fetch = { ZedMoviesResult.failure(error) },
            saveFetchResult = { error("nothing to save after a failure") }
        ).toList()

        assertEquals(3, results.size)
        assertEquals(ZedMoviesResult.Loading<String>(null), results[0])
        assertEquals(ZedMoviesResult.Loading("cached"), results[1])
        val failure = assertIs<ZedMoviesResult.Failure.Error<String>>(results[2])
        assertSame(error, failure.error)
        assertEquals("cached", failure.value)
    }

    @Test
    fun httpErrorEmitsTheCachedValueAsAFailure() = runTest {
        val error = HttpException(statusCode = 401, statusMessage = "Unauthorized", url = "https://example.test")
        val results = networkBoundResource<String, String>(
            query = { flowOf("cached") },
            fetch = { ZedMoviesResult.failure(error) },
            saveFetchResult = { error("nothing to save after a failure") }
        ).toList()

        // The failure is re-wrapped with the cached value (as a plain Failure.Error), the HttpException is kept.
        val failure = assertIs<ZedMoviesResult.Failure<String>>(results.last())
        assertSame(error, failure.error)
        assertEquals("cached", failure.value)
    }

    @Test
    fun freshCacheSkipsTheFetch() = runTest {
        val results = networkBoundResource<String, String>(
            query = { flowOf("cached") },
            fetch = { error("must not fetch") },
            saveFetchResult = { error("must not save") },
            shouldFetch = { false }
        ).toList()

        assertEquals(2, results.size)
        assertEquals(ZedMoviesResult.Loading<String>(null), results[0])
        assertEquals("cached", (assertIs<ZedMoviesResult.Success<String>>(results[1])).value)
    }
}
