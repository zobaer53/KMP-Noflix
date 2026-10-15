

package com.zobaer53.zedmovies.domain.usecase

import com.zobaer53.zedmovies.domain.model.WishlistModel
import com.zobaer53.zedmovies.domain.repository.TvShowDetailsRepository
import com.zobaer53.zedmovies.domain.repository.WishlistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest

class GetWishlistTvShowsUseCase(
    private val wishlistRepository: WishlistRepository,
    private val tvShowDetailsRepository: TvShowDetailsRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke() = wishlistRepository.getTvShows().flatMapLatest { wishlistTvShows ->
        val ids = wishlistTvShows.map(WishlistModel::id)
        tvShowDetailsRepository.getByIds(ids)
    }
}
