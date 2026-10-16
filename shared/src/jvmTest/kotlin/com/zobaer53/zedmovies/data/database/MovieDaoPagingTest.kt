package com.zobaer53.zedmovies.data.database

import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import com.zobaer53.zedmovies.data.database.model.common.Genre
import com.zobaer53.zedmovies.data.database.model.common.MediaType
import com.zobaer53.zedmovies.data.database.model.movie.MovieEntity
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class MovieDaoPagingTest {
    private val database = inMemoryDatabase()
    private val dao = database.movieDao

    @AfterTest
    fun close() = database.close()

    @Test
    fun pagesComeInInsertionOrder() = runTest {
        // The RemoteMediator inserts TMDB's pages in order; the list shows them in that order.
        dao.insertAll((1..5).map { movie(networkId = it, mediaType = MediaType.Movie.Popular) })
        dao.insertAll(listOf(movie(networkId = 99, mediaType = MediaType.Movie.TopRated)))
        val source = dao.getPagingByMediaType(MediaType.Movie.Popular)

        val first = source.load(LoadParams.Refresh(key = null, loadSize = 2, placeholdersEnabled = false))
        assertIs<LoadResult.Page<Int, MovieEntity>>(first)
        assertEquals(listOf(1, 2), first.data.map { it.networkId })
        assertNull(first.prevKey)

        val second = source.load(LoadParams.Append(key = checkNotNull(first.nextKey), loadSize = 2, placeholdersEnabled = false))
        assertIs<LoadResult.Page<Int, MovieEntity>>(second)
        assertEquals(listOf(3, 4), second.data.map { it.networkId })

        val last = source.load(LoadParams.Append(key = checkNotNull(second.nextKey), loadSize = 2, placeholdersEnabled = false))
        assertIs<LoadResult.Page<Int, MovieEntity>>(last)
        // Only the Popular rows: the TopRated one isn't in this list.
        assertEquals(listOf(5), last.data.map { it.networkId })
        assertNull(last.nextKey)
    }

    private fun movie(networkId: Int, mediaType: MediaType.Movie) = MovieEntity(
        mediaType = mediaType,
        networkId = networkId,
        title = "Movie $networkId",
        overview = "",
        popularity = 1.0,
        releaseDate = LocalDate(2024, 1, networkId.coerceIn(1, 28)),
        adult = false,
        genres = listOf(Genre.DRAMA),
        originalTitle = "Movie $networkId",
        originalLanguage = "en",
        voteAverage = 7.0,
        voteCount = 100,
        posterPath = null,
        backdropPath = null,
        video = false
    )
}
