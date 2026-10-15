package com.zobaer53.zedmovies.ui.screenshot

import com.zobaer53.zedmovies.ui.wishlist.WishlistScreen
import com.zobaer53.zedmovies.ui.wishlist.WishlistUiState
import org.junit.Test

class WishlistScreenshotTest : ScreenshotTest() {

    @Test
    fun wishlist() = wishlist(
        WishlistUiState(
            movies = listOf(
                fakeMovieDetails,
                fakeMovieDetails.copy(id = 2, title = "Paper Planes", voteAverage = 7.1),
                fakeMovieDetails.copy(id = 3, title = "Iron Harbor", voteAverage = 6.8)
            ),
            tvShows = listOf(fakeTvShowDetails)
        )
    )

    @Test
    fun wishlist_empty() = wishlist(WishlistUiState())

    private fun wishlist(uiState: WishlistUiState) {
        setScreen {
            WishlistScreen(
                uiState = uiState,
                onRefreshMovies = {},
                onRefreshTvShows = {},
                onMovieClick = {},
                onTvShowClick = {},
                onRetry = {},
                onOfflineModeClick = {}
            )
        }
        capture()
    }
}
