package com.zobaer53.zedmovies.ui.screenshot

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.paging.compose.collectAsLazyPagingItems
import com.zobaer53.zedmovies.data.model.FilterGenre
import com.zobaer53.zedmovies.data.model.SearchFilters
import com.zobaer53.zedmovies.ui.search.SearchScreen
import com.zobaer53.zedmovies.ui.search.SearchUiState
import org.junit.Test

class SearchScreenshotTest : ScreenshotTest() {

    private val suggestions = SearchUiState(movies = homeMovies(), tvShows = homeTvShows())

    @Test
    fun search() = search(suggestions)

    @Test
    fun search_results() = search(
        SearchUiState(
            query = "Night",
            isSearching = true,
            searchMovies = pagingOf(fakeMovies),
            searchTvShows = pagingOf(fakeTvShows)
        )
    )

    @Test
    fun search_active_filters() = search(
        SearchUiState(
            filters = SearchFilters(genres = setOf(FilterGenre.DRAMA), minRating = 5.0, yearTo = 2012),
            isSearching = true,
            searchMovies = pagingOf(fakeMovies),
            searchTvShows = pagingOf(fakeTvShows)
        )
    )

    @Test
    fun search_filters_sheet() {
        search(suggestions, capture = false)
        // The click needs a running clock to be dispatched; the sheet then settles on the fixed clock.
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithContentDescription("Filters").performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        settle()
        captureWithDialogs()
    }

    private fun search(uiState: SearchUiState, capture: Boolean = true) {
        setScreen {
            SearchScreen(
                uiState = uiState,
                searchMovies = uiState.searchMovies.collectAsLazyPagingItems(),
                searchTvShows = uiState.searchTvShows.collectAsLazyPagingItems(),
                onRefresh = {},
                onQueryChange = {},
                onApplyFilters = {},
                onRemoveFilter = {},
                onResetFilters = {},
                onLoadFilterOptions = {},
                onSeeAllClick = {},
                onMovieClick = {},
                onTvShowClick = {},
                onRetry = {},
                onOfflineModeClick = {}
            )
        }
        if (capture) capture()
    }
}
