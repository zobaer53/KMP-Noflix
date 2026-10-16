
package com.zobaer53.zedmovies.domain.usecase

import com.zobaer53.zedmovies.domain.model.MediaTypeModel
import com.zobaer53.zedmovies.domain.repository.TvShowRepository

class GetTvShowsPagingUseCase(private val repository: TvShowRepository) {
    operator fun invoke(mediaTypeModel: MediaTypeModel.TvShow) =
        repository.getPagingByMediaType(mediaTypeModel)
}
