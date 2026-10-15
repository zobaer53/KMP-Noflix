package com.zobaer53.zedmovies.data.network.api.service

import com.zobaer53.zedmovies.data.common.result.ZedMoviesResult
import com.zobaer53.zedmovies.data.network.model.response.TvShowResponseDto
import com.zobaer53.zedmovies.data.network.model.tvshow.NetworkTvShowDetails
import com.zobaer53.zedmovies.data.network.util.Constants.DEFAULT_PAGE
import com.zobaer53.zedmovies.data.network.util.Constants.Discover
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.APPEND_TO_RESPONSE
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.DETAILS_APPEND_TO_RESPONSE
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.INCLUDE_VIDEO_LANGUAGE
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.ID
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.LANGUAGE
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.PAGE
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.QUERY
import com.zobaer53.zedmovies.data.network.util.Constants.Path.DETAILS_TV_SHOW
import com.zobaer53.zedmovies.data.network.util.Constants.Path.DISCOVER_TV_SHOW
import com.zobaer53.zedmovies.data.network.util.Constants.Path.ON_THE_AIR_TV_SHOW
import com.zobaer53.zedmovies.data.network.util.Constants.Path.POPULAR_TV_SHOW
import com.zobaer53.zedmovies.data.network.util.Constants.Path.SEARCH_TV_SHOW
import com.zobaer53.zedmovies.data.network.util.Constants.Path.TOP_RATED_TV_SHOW
import com.zobaer53.zedmovies.data.network.util.Constants.Path.TRENDING_TV_SHOW
import com.zobaer53.zedmovies.data.network.util.safeGet
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter

/** TMDB TV endpoints. Same signatures as the former Retrofit interface. */
class TvShowService internal constructor(private val client: HttpClient) {
    suspend fun getTopRated(language: String, page: Int = DEFAULT_PAGE): ZedMoviesResult<TvShowResponseDto> =
        list(TOP_RATED_TV_SHOW, language, page)

    suspend fun getPopular(language: String, page: Int = DEFAULT_PAGE): ZedMoviesResult<TvShowResponseDto> =
        list(POPULAR_TV_SHOW, language, page)

    suspend fun getOnTheAir(language: String, page: Int = DEFAULT_PAGE): ZedMoviesResult<TvShowResponseDto> =
        list(ON_THE_AIR_TV_SHOW, language, page)

    suspend fun getDiscover(language: String, page: Int = DEFAULT_PAGE): ZedMoviesResult<TvShowResponseDto> =
        list(DISCOVER_TV_SHOW, language, page)

    /** [filters] are the discover parameters (see `Constants.Discover`). */
    suspend fun discover(
        filters: Map<String, String>,
        language: String,
        page: Int = DEFAULT_PAGE
    ): ZedMoviesResult<TvShowResponseDto> = client.safeGet(DISCOVER_TV_SHOW) {
        filters.forEach { (key, value) -> parameter(key, value) }
        parameter(LANGUAGE, language)
        parameter(PAGE, page)
    }

    suspend fun getTrending(language: String, page: Int = DEFAULT_PAGE): ZedMoviesResult<TvShowResponseDto> =
        list(TRENDING_TV_SHOW, language, page)

    suspend fun getDetailsById(
        id: Int,
        appendToResponse: String = DETAILS_APPEND_TO_RESPONSE,
        language: String,
        includeVideoLanguage: String
    ): ZedMoviesResult<NetworkTvShowDetails> = client.safeGet(DETAILS_TV_SHOW.replace("{$ID}", id.toString())) {
        parameter(APPEND_TO_RESPONSE, appendToResponse)
        parameter(LANGUAGE, language)
        parameter(INCLUDE_VIDEO_LANGUAGE, includeVideoLanguage)
    }

    suspend fun search(
        query: String,
        language: String,
        page: Int = DEFAULT_PAGE,
        firstAirDateYear: Int? = null
    ): ZedMoviesResult<TvShowResponseDto> = client.safeGet(SEARCH_TV_SHOW) {
        parameter(QUERY, query)
        parameter(LANGUAGE, language)
        parameter(PAGE, page)
        // Retrofit dropped null query parameters; so does Ktor, kept explicit.
        firstAirDateYear?.let { parameter(Discover.FIRST_AIR_DATE_YEAR, it) }
    }

    private suspend fun list(path: String, language: String, page: Int): ZedMoviesResult<TvShowResponseDto> =
        client.safeGet(path) {
            parameter(LANGUAGE, language)
            parameter(PAGE, page)
        }
}
