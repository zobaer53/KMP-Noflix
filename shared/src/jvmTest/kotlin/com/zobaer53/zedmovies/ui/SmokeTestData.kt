package com.zobaer53.zedmovies.ui

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.zobaer53.zedmovies.data.model.Cast
import com.zobaer53.zedmovies.data.model.Credits
import com.zobaer53.zedmovies.data.model.Crew
import com.zobaer53.zedmovies.data.model.FilterGenre
import com.zobaer53.zedmovies.data.model.Genre
import com.zobaer53.zedmovies.data.model.MediaType
import com.zobaer53.zedmovies.data.model.Movie
import com.zobaer53.zedmovies.data.model.MovieDetails
import com.zobaer53.zedmovies.data.model.ReleaseDate
import com.zobaer53.zedmovies.data.model.TvShow
import com.zobaer53.zedmovies.data.model.TvShowDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Same shapes as :app's screenshot FakeData. Poster and profile paths are null, so Coil goes straight
// to its fallback and the tests make no network calls.

private val crime = Genre(FilterGenre.CRIME.nameResourceId)
private val drama = Genre(FilterGenre.DRAMA.nameResourceId)

private const val SmokeOverview = "A detective with one night left before retirement takes on one last case."

internal val smokeMovies = listOf(
    smokeMovie(1, "The Long Night", "2024", 8.4),
    smokeMovie(2, "Paper Planes", "2023", 7.1)
)

internal val smokeTvShows = listOf(
    smokeTvShow(11, "Northern Lights", "2021", 8.8),
    smokeTvShow(12, "The Office Hours", "2019", 7.5)
)

internal fun smokeHomeMovies(): Map<MediaType.Movie, List<Movie>> =
    MediaType.Movie.entries.associateWith { smokeMovies }

internal fun smokeHomeTvShows(): Map<MediaType.TvShow, List<TvShow>> =
    MediaType.TvShow.entries.associateWith { smokeTvShows }

/** One fully loaded page, so no refresh indicator keeps the clock busy. */
internal fun <T : Any> smokePagingOf(items: List<T>): Flow<PagingData<T>> {
    val loaded = LoadState.NotLoading(endOfPaginationReached = true)
    return flowOf(PagingData.from(items, sourceLoadStates = LoadStates(loaded, loaded, loaded)))
}

private val smokeCredits = Credits(
    cast = listOf(Cast(id = 101, name = "Ana Rivera", character = "Detective Mora", profilePath = null)),
    crew = listOf(Crew(id = 201, name = "Sofia Lindqvist", job = "Director", profilePath = null))
)

internal val smokeMovieDetails = MovieDetails(
    id = 1,
    title = "The Long Night",
    overview = SmokeOverview,
    backdropPath = null,
    budget = 40_000_000,
    genres = listOf(drama, crime),
    posterPath = null,
    releaseDate = ReleaseDate(fullDate = "2024-03-15", year = "2024"),
    runtime = 128,
    video = false,
    voteAverage = 8.4,
    voteCount = 1_204,
    credits = smokeCredits,
    trailerKey = "trailer",
    isWishlisted = true
)

internal val smokeTvShowDetails = TvShowDetails(
    id = 11,
    name = "Northern Lights",
    backdropPath = null,
    episodeRunTime = 52,
    firstAirDate = ReleaseDate(fullDate = "2021-09-01", year = "2021"),
    genres = listOf(drama),
    numberOfEpisodes = 24,
    numberOfSeasons = 3,
    overview = SmokeOverview,
    popularity = 120.5,
    posterPath = null,
    voteAverage = 8.8,
    credits = smokeCredits,
    trailerKey = null,
    isWishlisted = false
)

private fun smokeMovie(id: Int, title: String, year: String, rating: Double) = Movie(
    id = id,
    title = title,
    overview = SmokeOverview,
    releaseDate = ReleaseDate(fullDate = "$year-01-01", year = year),
    genres = listOf(drama),
    voteAverage = rating,
    posterPath = null,
    backdropPath = null
)

private fun smokeTvShow(id: Int, name: String, year: String, rating: Double) = TvShow(
    id = id,
    name = name,
    overview = SmokeOverview,
    firstAirDate = ReleaseDate(fullDate = "$year-01-01", year = year),
    genres = listOf(drama),
    voteAverage = rating,
    posterPath = null,
    backdropPath = null
)
