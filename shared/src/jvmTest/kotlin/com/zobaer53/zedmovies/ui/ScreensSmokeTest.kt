package com.zobaer53.zedmovies.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.paging.compose.collectAsLazyPagingItems
import com.zobaer53.zedmovies.data.model.FilterGenre
import com.zobaer53.zedmovies.data.model.MediaType
import com.zobaer53.zedmovies.data.model.SearchFilters
import com.zobaer53.zedmovies.ui.designsystem.component.LocalSnackbarHostState
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme
import com.zobaer53.zedmovies.ui.details.DetailsScreen
import com.zobaer53.zedmovies.ui.details.DetailsUiState
import com.zobaer53.zedmovies.ui.home.HomeScreen
import com.zobaer53.zedmovies.ui.home.HomeUiState
import com.zobaer53.zedmovies.ui.list.ListScreen
import com.zobaer53.zedmovies.ui.list.ListUiState
import com.zobaer53.zedmovies.ui.playback.LocalMediaLauncher
import com.zobaer53.zedmovies.ui.playback.MediaLauncher
import com.zobaer53.zedmovies.ui.playback.PlayRequest
import com.zobaer53.zedmovies.ui.search.SearchScreen
import com.zobaer53.zedmovies.ui.search.SearchUiState
import com.zobaer53.zedmovies.ui.wishlist.WishlistScreen
import com.zobaer53.zedmovies.ui.wishlist.WishlistUiState
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Renders each screen's stateless composable on Compose Desktop (plan 4.5): the shared UI composes
 * off Android, with its resources, and shows the fake content. Pixels are checked by :app's
 * Robolectric screenshot tests, not here.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ScreensSmokeTest {

    private val defaultLocale = Locale.getDefault()

    @BeforeTest
    fun setUp() {
        // PlatformFormat's JVM actuals and the resource lookup both follow the default locale.
        Locale.setDefault(Locale.US)
        // collectAsLazyPagingItems collects on Dispatchers.Main, which plain JVM tests don't have.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun home() = screen(
        content = {
            HomeScreen(
                uiState = HomeUiState(movies = smokeHomeMovies(), tvShows = smokeHomeTvShows()),
                onSeeAllClick = {},
                onMovieClick = {},
                onTvShowClick = {},
                onRefresh = {},
                onRetry = {},
                onOfflineModeClick = {}
            )
        }
    ) {
        text("Upcoming Movies").assertExists()
        text("The Long Night").assertExists()
    }

    @Test
    fun list() = screen(
        content = {
            val uiState = ListUiState(
                mediaType = MediaType.Common.TopRated,
                movies = smokePagingOf(smokeMovies),
                tvShows = smokePagingOf(smokeTvShows)
            )
            ListScreen(
                uiState = uiState,
                movies = uiState.movies.collectAsLazyPagingItems(),
                tvShows = uiState.tvShows.collectAsLazyPagingItems(),
                onBackButtonClick = {},
                onMovieClick = {},
                onTvShowClick = {}
            )
        }
    ) {
        text("Top Rated").assertExists()
        text("The Long Night").assertExists()
    }

    @Test
    fun search_opens_the_filters_sheet() = screen(
        content = { Search(SearchUiState(movies = smokeHomeMovies(), tvShows = smokeHomeTvShows())) }
    ) {
        text("search place holder").assertExists()
        onNodeWithContentDescription("Filters").performClick()
        waitForIdle()
        // Literal tags: Maestro flow 07 finds the sliders by these ids (resource ids on Android).
        onNodeWithTag("filters_year_slider").assertExists()
        onNodeWithTag("filters_rating_slider").assertExists()
        text("Show results").assertExists()
    }

    @Test
    fun search_active_filters() = screen(
        content = {
            Search(
                SearchUiState(
                    filters = SearchFilters(genres = setOf(FilterGenre.DRAMA), minRating = 5.0),
                    isSearching = true,
                    searchMovies = smokePagingOf(smokeMovies),
                    searchTvShows = smokePagingOf(smokeTvShows)
                )
            )
        }
    ) {
        text("Drama").assertExists()
        text("5.0+ ★").assertExists()
        text("Clear all").assertExists()
    }

    @Test
    fun details_movie() = screen(
        content = { Details(DetailsUiState(mediaType = MediaType.Details.Movie(1), movie = smokeMovieDetails)) }
    ) {
        text("The Long Night").assertExists()
        text("Play").assertExists()
        text("Trailer").assertExists()
    }

    @Test
    fun details_tv_show() = screen(
        content = { Details(DetailsUiState(mediaType = MediaType.Details.TvShow(11), tvShow = smokeTvShowDetails)) }
    ) {
        text("Northern Lights").assertExists()
        // No trailer key, so no Trailer chip.
        onAllNodesWithText("Trailer", ignoreCase = true).assertCountEquals(0)
    }

    @Test
    fun wishlist() = screen(
        content = { Wishlist(WishlistUiState(movies = listOf(smokeMovieDetails), tvShows = listOf(smokeTvShowDetails))) }
    ) {
        text("The Long Night").assertExists()
    }

    @Test
    fun wishlist_empty() = screen(content = { Wishlist(WishlistUiState()) }) {
        text("Wish list is empty").assertExists()
    }

    @Composable
    private fun Search(uiState: SearchUiState) = SearchScreen(
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

    @Composable
    private fun Details(uiState: DetailsUiState) = DetailsScreen(
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

    @Composable
    private fun Wishlist(uiState: WishlistUiState) = WishlistScreen(
        uiState = uiState,
        onRefreshMovies = {},
        onRefreshTvShows = {},
        onMovieClick = {},
        onTvShowClick = {},
        onRetry = {},
        onOfflineModeClick = {}
    )

    /** Sets [content] inside the theme and the locals the app shell provides, then runs [checks]. */
    private fun screen(content: @Composable () -> Unit, checks: ComposeUiTest.() -> Unit) = runComposeUiTest {
        setContent {
            zedMoviesTheme(darkTheme = true) {
                CompositionLocalProvider(
                    LocalMediaLauncher provides NoOpMediaLauncher,
                    LocalSnackbarHostState provides remember { SnackbarHostState() }
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        content()
                    }
                }
            }
        }
        waitForIdle()
        checks()
    }

    // The host's light/dark mode picks values or values-dark, which differ in casing.
    private fun ComposeUiTest.text(text: String): SemanticsNodeInteraction =
        onAllNodesWithText(text, ignoreCase = true).onFirst()

    private object NoOpMediaLauncher : MediaLauncher {
        override fun play(request: PlayRequest) = Unit
        override fun playTrailer(youtubeKey: String) = Unit
    }
}
