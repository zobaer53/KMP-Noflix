package com.zobaer53.zedmovies.ui.screenshot

import androidx.paging.compose.collectAsLazyPagingItems
import com.zobaer53.zedmovies.data.model.MediaType
import com.zobaer53.zedmovies.ui.list.ListScreen
import com.zobaer53.zedmovies.ui.list.ListUiState
import org.junit.Test

class ListScreenshotTest : ScreenshotTest() {

    @Test
    fun see_all() {
        val uiState = ListUiState(
            mediaType = MediaType.Common.TopRated,
            movies = pagingOf(fakeMovies),
            tvShows = pagingOf(fakeTvShows)
        )
        setScreen {
            ListScreen(
                uiState = uiState,
                movies = uiState.movies.collectAsLazyPagingItems(),
                tvShows = uiState.tvShows.collectAsLazyPagingItems(),
                onBackButtonClick = {},
                onMovieClick = {},
                onTvShowClick = {}
            )
        }
        capture()
    }
}
