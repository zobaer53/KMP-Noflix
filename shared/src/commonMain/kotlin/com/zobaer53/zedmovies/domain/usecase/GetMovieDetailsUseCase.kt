

package com.zobaer53.zedmovies.domain.usecase

import com.zobaer53.zedmovies.domain.repository.MovieDetailsRepository

class GetMovieDetailsUseCase(private val repository: MovieDetailsRepository) {
    operator fun invoke(id: Int) = repository.getById(id)
}
