package com.zobaer53.zedmovies.web

import com.zobaer53.zedmovies.data.mapper.asImageUrl
import com.zobaer53.zedmovies.data.mapper.asTrailerKey
import com.zobaer53.zedmovies.data.network.model.common.NetworkCast
import com.zobaer53.zedmovies.data.network.model.common.NetworkCredits
import com.zobaer53.zedmovies.data.network.model.common.NetworkCrew
import com.zobaer53.zedmovies.data.network.model.common.NetworkGenreWithId
import com.zobaer53.zedmovies.data.network.model.movie.NetworkMovie
import com.zobaer53.zedmovies.data.network.model.movie.NetworkMovieDetails
import com.zobaer53.zedmovies.data.network.model.tvshow.NetworkTvShow
import com.zobaer53.zedmovies.data.network.model.tvshow.NetworkTvShowDetails
import com.zobaer53.zedmovies.domain.model.CastModel
import com.zobaer53.zedmovies.domain.model.CreditsModel
import com.zobaer53.zedmovies.domain.model.CrewModel
import com.zobaer53.zedmovies.domain.model.GenreModel
import com.zobaer53.zedmovies.domain.model.MovieDetailsModel
import com.zobaer53.zedmovies.domain.model.MovieModel
import com.zobaer53.zedmovies.domain.model.TvShowDetailsModel
import com.zobaer53.zedmovies.domain.model.TvShowModel

internal fun NetworkMovie.asMovieModel() = MovieModel(
    id = id,
    title = title,
    overview = overview,
    popularity = popularity,
    releaseDate = releaseDate,
    adult = adult,
    genres = genreIds.asGenreModels(),
    originalTitle = originalTitle,
    originalLanguage = originalLanguage,
    voteAverage = voteAverage,
    voteCount = voteCount,
    posterPath = posterPath?.asImageUrl(),
    backdropPath = backdropPath?.asImageUrl(),
    video = video
)

internal fun NetworkTvShow.asTvShowModel() = TvShowModel(
    id = id,
    name = name,
    overview = overview,
    popularity = popularity,
    firstAirDate = firstAirDate,
    genres = genreIds.asGenreModels(),
    originalName = originalName,
    originalLanguage = originalLanguage,
    originCountry = originCountry,
    voteAverage = voteAverage,
    voteCount = voteCount,
    posterPath = posterPath?.asImageUrl(),
    backdropPath = backdropPath?.asImageUrl()
)

internal fun NetworkMovieDetails.asMovieDetailsModel(language: String, isWishlisted: Boolean) = MovieDetailsModel(
    id = id,
    adult = adult,
    backdropPath = backdropPath?.asImageUrl(),
    budget = budget,
    genres = genres.mapNotNull { NetworkGenreWithId[it.id]?.genreName?.let(GenreModel::get) },
    homepage = homepage,
    imdbId = imdbId,
    originalLanguage = originalLanguage,
    originalTitle = originalTitle,
    overview = overview,
    popularity = popularity,
    posterPath = posterPath?.asImageUrl(),
    releaseDate = releaseDate,
    revenue = revenue,
    runtime = runtime,
    status = status,
    tagline = tagline,
    title = title,
    video = video,
    voteAverage = voteAverage,
    voteCount = voteCount,
    credits = credits.asCreditsModel(),
    trailerKey = videos.asTrailerKey(language),
    isWishlisted = isWishlisted
)

internal fun NetworkTvShowDetails.asTvShowDetailsModel(language: String, isWishlisted: Boolean) =
    TvShowDetailsModel(
        id = id,
        name = name,
        adult = adult,
        backdropPath = backdropPath?.asImageUrl(),
        episodeRunTime = episodeRunTime,
        firstAirDate = firstAirDate,
        genres = genres.mapNotNull { NetworkGenreWithId[it.id]?.genreName?.let(GenreModel::get) },
        homepage = homepage,
        inProduction = inProduction,
        languages = languages,
        lastAirDate = lastAirDate,
        numberOfEpisodes = numberOfEpisodes,
        numberOfSeasons = numberOfSeasons,
        originCountry = originCountry,
        originalLanguage = originalLanguage,
        originalName = originalName,
        overview = overview,
        popularity = popularity,
        posterPath = posterPath?.asImageUrl(),
        status = status,
        tagline = tagline,
        type = type,
        voteAverage = voteAverage,
        voteCount = voteCount,
        credits = credits.asCreditsModel(),
        trailerKey = videos.asTrailerKey(language),
        isWishlisted = isWishlisted
    )

private fun List<Int>.asGenreModels() = mapNotNull { NetworkGenreWithId[it]?.genreName?.let(GenreModel::get) }

private fun NetworkCredits.asCreditsModel() = CreditsModel(
    cast = cast.map { it.asCastModel() },
    crew = crew.map { it.asCrewModel() }
)

private fun NetworkCast.asCastModel() = CastModel(
    id = id,
    name = name,
    adult = adult,
    castId = 0,
    character = character,
    creditId = creditId,
    gender = gender,
    knownForDepartment = knownForDepartment,
    order = order,
    originalName = originalName,
    popularity = popularity,
    profilePath = profilePath?.asImageUrl()
)

private fun NetworkCrew.asCrewModel() = CrewModel(
    id = id,
    adult = adult,
    creditId = creditId,
    department = department,
    gender = gender,
    job = job,
    knownForDepartment = knownForDepartment,
    name = name,
    originalName = originalName,
    popularity = popularity,
    profilePath = profilePath?.asImageUrl()
)
