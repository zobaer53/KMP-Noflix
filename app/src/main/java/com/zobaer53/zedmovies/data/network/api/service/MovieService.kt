

package com.zobaer53.zedmovies.data.network.api.service

import com.zobaer53.zedmovies.data.common.result.ZedMoviesResult
import com.zobaer53.zedmovies.data.network.model.movie.NetworkMovieDetails
import com.zobaer53.zedmovies.data.network.model.response.MovieResponseDto
import com.zobaer53.zedmovies.data.network.util.Constants.DEFAULT_PAGE
import com.zobaer53.zedmovies.data.network.util.Constants.Discover
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.APPEND_TO_RESPONSE
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.DETAILS_APPEND_TO_RESPONSE
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.INCLUDE_VIDEO_LANGUAGE
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.ID
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.LANGUAGE
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.PAGE
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.QUERY
import com.zobaer53.zedmovies.data.network.util.Constants.Path.DETAILS_MOVIE
import com.zobaer53.zedmovies.data.network.util.Constants.Path.DISCOVER_MOVIE
import com.zobaer53.zedmovies.data.network.util.Constants.Path.NOW_PLAYING_MOVIE
import com.zobaer53.zedmovies.data.network.util.Constants.Path.POPULAR_MOVIE
import com.zobaer53.zedmovies.data.network.util.Constants.Path.SEARCH_MOVIE
import com.zobaer53.zedmovies.data.network.util.Constants.Path.TOP_RATED_MOVIE
import com.zobaer53.zedmovies.data.network.util.Constants.Path.TRENDING_MOVIE
import com.zobaer53.zedmovies.data.network.util.Constants.Path.UPCOMING_MOVIE
import com.zobaer53.zedmovies.data.network.util.safeGet
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter

/** TMDB movie endpoints. Same signatures as the former Retrofit interface. */
class MovieService internal constructor(private val client: HttpClient) {
    suspend fun getUpcoming(language: String, page: Int = DEFAULT_PAGE): ZedMoviesResult<MovieResponseDto> =
        list(UPCOMING_MOVIE, language, page)

    suspend fun getTopRated(language: String, page: Int = DEFAULT_PAGE): ZedMoviesResult<MovieResponseDto> =
        list(TOP_RATED_MOVIE, language, page)

    suspend fun getPopular(language: String, page: Int = DEFAULT_PAGE): ZedMoviesResult<MovieResponseDto> =
        list(POPULAR_MOVIE, language, page)

    suspend fun getNowPlaying(language: String, page: Int = DEFAULT_PAGE): ZedMoviesResult<MovieResponseDto> =
        list(NOW_PLAYING_MOVIE, language, page)

    suspend fun getDiscover(language: String, page: Int = DEFAULT_PAGE): ZedMoviesResult<MovieResponseDto> =
        list(DISCOVER_MOVIE, language, page)

    /** [filters] are the discover parameters (see `Constants.Discover`). */
    suspend fun discover(
        filters: Map<String, String>,
        language: String,
        page: Int = DEFAULT_PAGE
    ): ZedMoviesResult<MovieResponseDto> = client.safeGet(DISCOVER_MOVIE) {
        filters.forEach { (key, value) -> parameter(key, value) }
        parameter(LANGUAGE, language)
        parameter(PAGE, page)
    }

    suspend fun getTrending(language: String, page: Int = DEFAULT_PAGE): ZedMoviesResult<MovieResponseDto> =
        list(TRENDING_MOVIE, language, page)

    suspend fun getDetailsById(
        id: Int,
        language: String,
        includeVideoLanguage: String,
        appendToResponse: String = DETAILS_APPEND_TO_RESPONSE
    ): ZedMoviesResult<NetworkMovieDetails> = client.safeGet(DETAILS_MOVIE.replace("{$ID}", id.toString())) {
        parameter(LANGUAGE, language)
        parameter(INCLUDE_VIDEO_LANGUAGE, includeVideoLanguage)
        parameter(APPEND_TO_RESPONSE, appendToResponse)
    }

    suspend fun search(
        query: String,
        language: String,
        page: Int = DEFAULT_PAGE,
        primaryReleaseYear: Int? = null
    ): ZedMoviesResult<MovieResponseDto> = client.safeGet(SEARCH_MOVIE) {
        parameter(QUERY, query)
        parameter(LANGUAGE, language)
        parameter(PAGE, page)
        // Retrofit dropped null query parameters; so does Ktor, kept explicit.
        primaryReleaseYear?.let { parameter(Discover.PRIMARY_RELEASE_YEAR, it) }
    }

    private suspend fun list(path: String, language: String, page: Int): ZedMoviesResult<MovieResponseDto> =
        client.safeGet(path) {
            parameter(LANGUAGE, language)
            parameter(PAGE, page)
        }
}
