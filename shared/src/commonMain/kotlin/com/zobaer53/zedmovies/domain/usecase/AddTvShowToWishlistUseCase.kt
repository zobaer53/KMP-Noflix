
package com.zobaer53.zedmovies.domain.usecase

import com.zobaer53.zedmovies.domain.repository.WishlistRepository

class AddTvShowToWishlistUseCase(private val repository: WishlistRepository) {
    suspend operator fun invoke(id: Int) = repository.addTvShowToWishlist(id)
}
