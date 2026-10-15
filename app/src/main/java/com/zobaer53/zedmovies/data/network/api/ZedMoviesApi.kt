
package com.zobaer53.zedmovies.data.network.api

import com.zobaer53.zedmovies.data.network.api.service.FilterOptionsService
import com.zobaer53.zedmovies.data.network.api.service.MovieService
import com.zobaer53.zedmovies.data.network.api.service.TvShowService
import com.zobaer53.zedmovies.data.network.util.zedMoviesHttpClient
import io.ktor.client.HttpClient

/** One HTTP client for TMDB and the three services on top of it. */
class zedMoviesApi internal constructor(private val client: HttpClient) {
    constructor(apiKeyProvider: zedMoviesApiKeyProvider) : this(zedMoviesHttpClient(apiKeyProvider))

    val movieService: MovieService by lazy { MovieService(client) }
    val tvShowService: TvShowService by lazy { TvShowService(client) }
    val filterOptionsService: FilterOptionsService by lazy { FilterOptionsService(client) }
}
