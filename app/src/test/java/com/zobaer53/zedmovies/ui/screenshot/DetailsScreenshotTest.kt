package com.zobaer53.zedmovies.ui.screenshot

import com.zobaer53.zedmovies.data.model.MediaType
import com.zobaer53.zedmovies.ui.details.DetailsScreen
import com.zobaer53.zedmovies.ui.details.DetailsUiState
import org.junit.Test

class DetailsScreenshotTest : ScreenshotTest() {

    @Test
    fun details_movie() = details(
        DetailsUiState(mediaType = MediaType.Details.Movie(fakeMovieDetails.id), movie = fakeMovieDetails)
    )

    @Test
    fun details_tv_show() = details(
        DetailsUiState(mediaType = MediaType.Details.TvShow(fakeTvShowDetails.id), tvShow = fakeTvShowDetails)
    )

    @Test
    fun details_loading() = details(
        DetailsUiState(mediaType = MediaType.Details.Movie(fakeMovieDetails.id), isLoading = true)
    )

    private fun details(uiState: DetailsUiState) {
        setScreen {
            DetailsScreen(
                uiState = uiState,
                onShowMessage = {},
                onRefresh = {},
                onBackButtonClick = {},
                onWishlistMovieClick = {},
                onWishlistTvShowClick = {},
                onRetry = {},
                onOfflineModeClick = {},
                onUserMessageDismiss = {}
            )
        }
        capture()
    }
}
