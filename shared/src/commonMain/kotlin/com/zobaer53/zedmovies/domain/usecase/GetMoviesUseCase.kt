

package com.zobaer53.zedmovies.domain.usecase

import com.zobaer53.zedmovies.domain.model.MediaTypeModel
import com.zobaer53.zedmovies.domain.repository.MovieRepository

class GetMoviesUseCase(private val repository: MovieRepository) {
    operator fun invoke(mediaTypeModel: MediaTypeModel.Movie) =
        repository.getByMediaType(mediaTypeModel)
}
