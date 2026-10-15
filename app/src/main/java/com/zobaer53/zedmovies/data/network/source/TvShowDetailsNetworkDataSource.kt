
package com.zobaer53.zedmovies.data.network.source

import com.zobaer53.zedmovies.data.common.result.ZedMoviesResult
import com.zobaer53.zedmovies.data.common.result.isFailure
import com.zobaer53.zedmovies.data.common.result.isSuccess
import com.zobaer53.zedmovies.data.network.api.service.TvShowService
import com.zobaer53.zedmovies.data.network.model.tvshow.NetworkTvShowDetails
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.DETAILS_APPEND_TO_RESPONSE
import com.zobaer53.zedmovies.data.network.util.Constants.Fields.buildIncludeVideoLanguage
import com.zobaer53.zedmovies.data.network.util.MESSAGE_UNHANDLED_STATE
import javax.inject.Inject

class TvShowDetailsNetworkDataSource @Inject constructor(private val tvShowService: TvShowService) {
    suspend fun getById(
        id: Int,
        language: String,
        appendToResponse: String = DETAILS_APPEND_TO_RESPONSE
    ): ZedMoviesResult<NetworkTvShowDetails> =
        tvShowService.getDetailsById(id, appendToResponse, language, buildIncludeVideoLanguage(language))

    suspend fun getByIds(
        ids: List<Int>,
        language: String,
        appendToResponse: String = DETAILS_APPEND_TO_RESPONSE
    ): ZedMoviesResult<List<NetworkTvShowDetails>> {
        val tvShows = ids.map { id ->
            val response = tvShowService.getDetailsById(id, appendToResponse, language, buildIncludeVideoLanguage(language))
            when {
                response.isSuccess() -> response.value
                response.isFailure() -> return ZedMoviesResult.failure(response.error)
                else ->
                    error("$MESSAGE_UNHANDLED_STATE $response")
            }
        }

        return ZedMoviesResult.success(tvShows)
    }
}
