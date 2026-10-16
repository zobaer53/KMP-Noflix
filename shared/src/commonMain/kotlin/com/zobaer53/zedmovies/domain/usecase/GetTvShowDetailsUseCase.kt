
package com.zobaer53.zedmovies.domain.usecase

import com.zobaer53.zedmovies.domain.repository.TvShowDetailsRepository

class GetTvShowDetailsUseCase(private val repository: TvShowDetailsRepository) {
    operator fun invoke(id: Int) = repository.getById(id)
}
