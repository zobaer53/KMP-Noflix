
package com.zobaer53.zedmovies.ui.smallcomponent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.zobaer53.zedmovies.data.model.Movie
import com.zobaer53.zedmovies.data.model.TvShow
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.no_movie_results
import com.zobaer53.zedmovies.resources.no_search_results
import com.zobaer53.zedmovies.resources.no_tv_show_results
import com.zobaer53.zedmovies.ui.designsystem.component.ZedMoviesSwipeRefresh
import com.zobaer53.zedmovies.ui.designsystem.component.zedMoviesCenteredBox
import com.zobaer53.zedmovies.ui.designsystem.component.zedMoviesCircularProgressIndicator
import com.zobaer53.zedmovies.ui.designsystem.component.zedMoviesMessage
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme
import com.zobaer53.zedmovies.ui.model.UserMessage
import com.zobaer53.zedmovies.ui.smallcomponent.mapper.asUserMessage
import com.zobaer53.zedmovies.ui.smallcomponent.util.error
import com.zobaer53.zedmovies.ui.smallcomponent.util.isEmpty
import com.zobaer53.zedmovies.ui.smallcomponent.util.isError
import com.zobaer53.zedmovies.ui.smallcomponent.util.isFinished
import com.zobaer53.zedmovies.ui.smallcomponent.util.isLoading
import com.zobaer53.zedmovies.ui.smallcomponent.util.isNotEmpty

/**
 * Multi-column counterpart of [MoviesContainer] for wide screens. Column count is chosen by the
 * host from the window width; the phone layout keeps the single-column list.
 */
@Composable
fun MoviesGrid(
    movies: LazyPagingItems<Movie>,
    onClick: (Int) -> Unit,
    columnCount: Int,
    modifier: Modifier = Modifier
) {
    ZedMoviesSwipeRefresh(
        modifier = modifier,
        isRefreshing = movies.loadState.refresh.isLoading,
        onRefresh = movies::refresh
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(zedMoviesTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(zedMoviesTheme.spacing.medium),
            contentPadding = PaddingValues(zedMoviesTheme.spacing.extraMedium)
        ) {
            when {
                movies.isNotEmpty() -> {
                    items(
                        count = movies.itemCount,
                        key = movies.itemKey(),
                        contentType = movies.itemContentType()
                    ) { index ->
                        val movie = movies[index]
                        if (movie == null) {
                            VerticalMovieItemPlaceholder()
                        } else {
                            VerticalMovieItem(movie = movie, onClick = onClick)
                        }
                    }
                }
                movies.loadState.refresh.isLoading -> {
                    items(PlaceholderCount) { VerticalMovieItemPlaceholder() }
                }
                movies.loadState.refresh.isFinished && movies.isEmpty() -> {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        zedMoviesMessage(
                            modifier = Modifier.fillMaxSize(),
                            messageResourceId = Res.string.no_movie_results,
                            imageResourceId = Res.drawable.no_search_results
                        )
                    }
                }
                movies.loadState.refresh.isError -> {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        zedMoviesCenteredBox(modifier = Modifier.fillMaxSize()) {
                            zedMoviesError(
                                modifier = Modifier.fillMaxSize(),
                                errorMessage = movies.loadState.refresh.error.asUserMessage(),
                                onRetry = movies::retry
                            )
                        }
                    }
                }
            }
            if (movies.loadState.append.isLoading) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    zedMoviesCenteredBox(modifier = Modifier.fillMaxSize()) {
                        zedMoviesCircularProgressIndicator()
                    }
                }
            }
        }
    }
}

/** TV-grid counterpart of [MoviesGrid]. */
@Composable
fun TvShowsGrid(
    tvShows: LazyPagingItems<TvShow>,
    onClick: (Int) -> Unit,
    columnCount: Int,
    modifier: Modifier = Modifier
) {
    ZedMoviesSwipeRefresh(
        modifier = modifier,
        isRefreshing = tvShows.loadState.refresh.isLoading,
        onRefresh = tvShows::refresh
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(zedMoviesTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(zedMoviesTheme.spacing.medium),
            contentPadding = PaddingValues(zedMoviesTheme.spacing.extraMedium)
        ) {
            when {
                tvShows.isNotEmpty() -> {
                    items(
                        count = tvShows.itemCount,
                        key = tvShows.itemKey(),
                        contentType = tvShows.itemContentType()
                    ) { index ->
                        val tvShow = tvShows[index]
                        if (tvShow == null) {
                            VerticalTvShowItemPlaceholder()
                        } else {
                            VerticalTvShowItem(tvShow = tvShow, onClick = onClick)
                        }
                    }
                }
                tvShows.loadState.refresh.isLoading -> {
                    items(PlaceholderCount) { VerticalTvShowItemPlaceholder() }
                }
                tvShows.loadState.refresh.isFinished && tvShows.isEmpty() -> {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        zedMoviesMessage(
                            modifier = Modifier.fillMaxSize(),
                            messageResourceId = Res.string.no_tv_show_results,
                            imageResourceId = Res.drawable.no_search_results
                        )
                    }
                }
                tvShows.loadState.refresh.isError -> {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        zedMoviesCenteredBox(modifier = Modifier.fillMaxSize()) {
                            zedMoviesError(
                                modifier = Modifier.fillMaxSize(),
                                errorMessage = tvShows.loadState.refresh.error.asUserMessage(),
                                onRetry = tvShows::retry
                            )
                        }
                    }
                }
            }
            if (tvShows.loadState.append.isLoading) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    zedMoviesCenteredBox(modifier = Modifier.fillMaxSize()) {
                        zedMoviesCircularProgressIndicator()
                    }
                }
            }
        }
    }
}

private const val PlaceholderCount = 20
