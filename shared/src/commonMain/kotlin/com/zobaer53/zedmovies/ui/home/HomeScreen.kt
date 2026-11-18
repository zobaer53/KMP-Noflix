
package com.zobaer53.zedmovies.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.most_popular
import com.zobaer53.zedmovies.resources.now_playing
import com.zobaer53.zedmovies.resources.refresh
import com.zobaer53.zedmovies.resources.top_rated
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zobaer53.zedmovies.ui.designsystem.component.ZedMoviesSwipeRefresh
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme
import com.zobaer53.zedmovies.data.model.MediaType
import com.zobaer53.zedmovies.data.model.Movie
import com.zobaer53.zedmovies.data.model.TvShow
import com.zobaer53.zedmovies.ui.smallcomponent.MoviesAndTvShowsContainer
import com.zobaer53.zedmovies.ui.home.component.UpcomingMoviesContainer
import com.zobaer53.zedmovies.ui.adaptive.InputMode
import com.zobaer53.zedmovies.ui.adaptive.LocalInputMode

@Composable
internal fun HomeRoute(
    onSeeAllClick: (MediaType.Common) -> Unit,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onSeeAllClick = onSeeAllClick,
        onMovieClick = onMovieClick,
        onTvShowClick = onTvShowClick,
        onRefresh = { viewModel.onEvent(HomeEvent.Refresh) },
        onRetry = { viewModel.onEvent(HomeEvent.Retry) },
        onOfflineModeClick = { viewModel.onEvent(HomeEvent.ClearError) },
        modifier = modifier
    )
}

// Public (not internal) so :app's screenshot tests can render it.
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSeeAllClick: (MediaType.Common) -> Unit,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOfflineModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ZedMoviesSwipeRefresh(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        isRefreshing = uiState.isLoading,
        onRefresh = onRefresh
    ) {
        HomeContent(
            movies = uiState.movies,
            tvShows = uiState.tvShows,
            onSeeAllClick = onSeeAllClick,
            onMovieClick = onMovieClick,
            onTvShowClick = onTvShowClick,
            onRefresh = onRefresh
        )
    }
}

@Composable
private fun HomeContent(
    movies: Map<MediaType.Movie, List<Movie>>,
    tvShows: Map<MediaType.TvShow, List<TvShow>>,
    onSeeAllClick: (MediaType.Common) -> Unit,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inputMode = LocalInputMode.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(zedMoviesTheme.spacing.extraMedium),
        contentPadding = PaddingValues(vertical = zedMoviesTheme.spacing.extraMedium)
    ) {
        // Touch users pull to refresh; keyboard/D-pad users get a focusable action instead.
        if (inputMode != InputMode.Touch) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onRefresh) {
                        Text(
                            text = stringResource(Res.string.refresh),
                            style = zedMoviesTheme.typography.medium.h5,
                            color = zedMoviesTheme.colors.accent
                        )
                    }
                }
            }
        }
        item {
            UpcomingMoviesContainer(
                movies = movies[MediaType.Movie.Upcoming].orEmpty(),
                onSeeAllClick = { onSeeAllClick(MediaType.Common.Upcoming) },
                onMovieClick = onMovieClick
            )
        }
        item {
            MoviesAndTvShowsContainer(
                titleResourceId = Res.string.top_rated,
                onSeeAllClick = { onSeeAllClick(MediaType.Common.TopRated) },
                movies = movies[MediaType.Movie.TopRated].orEmpty(),
                tvShows = tvShows[MediaType.TvShow.TopRated].orEmpty(),
                onMovieClick = onMovieClick,
                onTvShowClick = onTvShowClick
            )
        }
        item {
            MoviesAndTvShowsContainer(
                titleResourceId = Res.string.most_popular,
                onSeeAllClick = { onSeeAllClick(MediaType.Common.Popular) },
                movies = movies[MediaType.Movie.Popular].orEmpty(),
                tvShows = tvShows[MediaType.TvShow.Popular].orEmpty(),
                onMovieClick = onMovieClick,
                onTvShowClick = onTvShowClick
            )
        }
        item {
            MoviesAndTvShowsContainer(
                titleResourceId = Res.string.now_playing,
                onSeeAllClick = { onSeeAllClick(MediaType.Common.NowPlaying) },
                movies = movies[MediaType.Movie.NowPlaying].orEmpty(),
                tvShows = tvShows[MediaType.TvShow.NowPlaying].orEmpty(),
                onMovieClick = onMovieClick,
                onTvShowClick = onTvShowClick
            )
        }
    }
}
