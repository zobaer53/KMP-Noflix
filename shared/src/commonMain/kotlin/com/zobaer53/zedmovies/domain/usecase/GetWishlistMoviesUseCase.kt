

package com.zobaer53.zedmovies.domain.usecase

import com.zobaer53.zedmovies.domain.model.WishlistModel
import com.zobaer53.zedmovies.domain.repository.MovieDetailsRepository
import com.zobaer53.zedmovies.domain.repository.WishlistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest

class GetWishlistMoviesUseCase(
    private val wishlistRepository: WishlistRepository,
    private val movieDetailsRepository: MovieDetailsRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke() = wishlistRepository.getMovies().flatMapLatest { wishlistMovies ->
        val ids = wishlistMovies.map(WishlistModel::id)
        movieDetailsRepository.getByIds(ids)
    }
}
