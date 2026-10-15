package com.zobaer53.zedmovies.data.network.api.service

import com.zobaer53.zedmovies.data.common.result.ZedMoviesResult
import com.zobaer53.zedmovies.data.network.model.common.NetworkCertificationsResponse
import com.zobaer53.zedmovies.data.network.model.common.NetworkWatchProvidersResponse
import com.zobaer53.zedmovies.data.network.util.Constants.Discover.WATCH_REGION
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.LANGUAGE
import com.zobaer53.zedmovies.data.network.util.Constants.Path.MOVIE_CERTIFICATIONS
import com.zobaer53.zedmovies.data.network.util.Constants.Path.MOVIE_WATCH_PROVIDERS
import com.zobaer53.zedmovies.data.network.util.Constants.Path.TV_WATCH_PROVIDERS
import com.zobaer53.zedmovies.data.network.util.safeGet
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter

/** Choices for the Search tab's filters that TMDB maintains. */
class FilterOptionsService internal constructor(private val client: HttpClient) {
    suspend fun getMovieCertifications(): ZedMoviesResult<NetworkCertificationsResponse> =
        client.safeGet(MOVIE_CERTIFICATIONS)

    suspend fun getMovieWatchProviders(region: String, language: String): ZedMoviesResult<NetworkWatchProvidersResponse> =
        client.safeGet(MOVIE_WATCH_PROVIDERS) {
            parameter(WATCH_REGION, region)
            parameter(LANGUAGE, language)
        }

    suspend fun getTvWatchProviders(region: String, language: String): ZedMoviesResult<NetworkWatchProvidersResponse> =
        client.safeGet(TV_WATCH_PROVIDERS) {
            parameter(WATCH_REGION, region)
            parameter(LANGUAGE, language)
        }
}
