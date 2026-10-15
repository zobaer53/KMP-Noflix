package com.zobaer53.zedmovies.ui.screenshot

import com.zobaer53.zedmovies.data.model.MediaType
import com.zobaer53.zedmovies.ui.home.HomeScreen
import com.zobaer53.zedmovies.ui.home.HomeUiState
import org.junit.Test

class HomeScreenshotTest : ScreenshotTest() {

    @Test
    fun home() = home(HomeUiState(movies = homeMovies(), tvShows = homeTvShows()))

    @Test
    fun home_loading() = home(
        HomeUiState(loadStates = (MediaType.Movie.entries + MediaType.TvShow.entries).associateWith { true })
    )

    private fun home(uiState: HomeUiState) {
        setScreen {
            HomeScreen(
                uiState = uiState,
                onSeeAllClick = {},
                onMovieClick = {},
                onTvShowClick = {},
                onRefresh = {},
                onRetry = {},
                onOfflineModeClick = {}
            )
        }
        capture()
    }
}
