package com.zobaer53.zedmovies.ui.screenshot

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.zobaer53.zedmovies.data.model.Cast
import com.zobaer53.zedmovies.data.model.Credits
import com.zobaer53.zedmovies.data.model.Crew
import com.zobaer53.zedmovies.data.model.Genre
import com.zobaer53.zedmovies.data.model.MediaType
import com.zobaer53.zedmovies.data.model.Movie
import com.zobaer53.zedmovies.data.model.MovieDetails
import com.zobaer53.zedmovies.data.model.ReleaseDate
import com.zobaer53.zedmovies.data.model.TvShow
import com.zobaer53.zedmovies.data.model.TvShowDetails
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.action
import com.zobaer53.zedmovies.resources.adventure
import com.zobaer53.zedmovies.resources.comedy
import com.zobaer53.zedmovies.resources.crime
import com.zobaer53.zedmovies.resources.drama
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Fixed, made-up content for the screenshot tests. Posters are null, so Coil draws its placeholder
// and nothing goes to the network.

private val action = Genre(Res.string.action)
private val adventure = Genre(Res.string.adventure)
private val comedy = Genre(Res.string.comedy)
private val crime = Genre(Res.string.crime)
private val drama = Genre(Res.string.drama)

internal val fakeMovies = listOf(
    fakeMovie(1, "The Long Night", "2024", 8.4, listOf(drama, crime)),
    fakeMovie(2, "Paper Planes", "2023", 7.1, listOf(comedy, drama)),
    fakeMovie(3, "Iron Harbor", "2025", 6.8, listOf(action, adventure)),
    fakeMovie(4, "Quiet Streets", "2022", 7.9, listOf(crime)),
    fakeMovie(5, "Summer of Glass", "2025", 0.0, listOf(drama))
)

internal val fakeTvShows = listOf(
    fakeTvShow(11, "Northern Lights", "2021", 8.8, listOf(drama)),
    fakeTvShow(12, "The Office Hours", "2019", 7.5, listOf(comedy)),
    fakeTvShow(13, "Cold Case Files", "2024", 8.1, listOf(crime, drama)),
    fakeTvShow(14, "Sky Pirates", "2023", 6.9, listOf(action, adventure))
)

internal fun homeMovies(): Map<MediaType.Movie, List<Movie>> =
    MediaType.Movie.entries.associateWith { fakeMovies }

internal fun homeTvShows(): Map<MediaType.TvShow, List<TvShow>> =
    MediaType.TvShow.entries.associateWith { fakeTvShows }

/** One fully loaded page: every load state is "not loading, end reached", so no refresh indicator shows. */
internal fun <T : Any> pagingOf(items: List<T>): Flow<PagingData<T>> {
    val loaded = LoadState.NotLoading(endOfPaginationReached = true)
    return flowOf(PagingData.from(items, sourceLoadStates = LoadStates(loaded, loaded, loaded)))
}

private val fakeCredits = Credits(
    cast = listOf(
        Cast(id = 101, name = "Ana Rivera", character = "Detective Mora", profilePath = null),
        Cast(id = 102, name = "James Okafor", character = "Leon", profilePath = null),
        Cast(id = 103, name = "Mei Tanaka", character = "Hana", profilePath = null)
    ),
    crew = listOf(
        Crew(id = 201, name = "Sofia Lindqvist", job = "Director", profilePath = null),
        Crew(id = 202, name = "Tom Becker", job = "Writer", profilePath = null)
    )
)

private const val FakeOverview = "A detective with one night left before retirement takes on the case that " +
    "has haunted the city for twenty years, and finds that the answer was closer than anyone thought."

internal val fakeMovieDetails = MovieDetails(
    id = 1,
    title = "The Long Night",
    overview = FakeOverview,
    backdropPath = null,
    budget = 40_000_000,
    genres = listOf(drama, crime),
    posterPath = null,
    releaseDate = ReleaseDate(fullDate = "2024-03-15", year = "2024"),
    runtime = 128,
    video = false,
    voteAverage = 8.4,
    voteCount = 1_204,
    credits = fakeCredits,
    trailerKey = "trailer",
    isWishlisted = true
)

internal val fakeTvShowDetails = TvShowDetails(
    id = 11,
    name = "Northern Lights",
    backdropPath = null,
    episodeRunTime = 52,
    firstAirDate = ReleaseDate(fullDate = "2021-09-01", year = "2021"),
    genres = listOf(drama),
    numberOfEpisodes = 24,
    numberOfSeasons = 3,
    overview = FakeOverview,
    popularity = 120.5,
    posterPath = null,
    voteAverage = 8.8,
    credits = fakeCredits,
    trailerKey = null,
    isWishlisted = false
)

private fun fakeMovie(id: Int, title: String, year: String, rating: Double, genres: List<Genre>) = Movie(
    id = id,
    title = title,
    overview = FakeOverview,
    releaseDate = ReleaseDate(fullDate = "$year-01-01", year = year),
    genres = genres,
    voteAverage = rating,
    posterPath = null,
    backdropPath = null
)

private fun fakeTvShow(id: Int, name: String, year: String, rating: Double, genres: List<Genre>) = TvShow(
    id = id,
    name = name,
    overview = FakeOverview,
    firstAirDate = ReleaseDate(fullDate = "$year-01-01", year = year),
    genres = genres,
    voteAverage = rating,
    posterPath = null,
    backdropPath = null
)
