

package com.zobaer53.zedmovies.ui.details.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zobaer53.zedmovies.ui.smallcomponent.mapper.asNames
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.details_runtime_text
import com.zobaer53.zedmovies.resources.no_runtime
import com.zobaer53.zedmovies.data.model.MovieDetails
import com.zobaer53.zedmovies.ui.smallcomponent.mapper.NoMovieRuntimeValue
import org.jetbrains.compose.resources.stringResource

@Suppress("ReusedModifierInstance")
@Composable
internal fun MovieDetailsItem(
    movieDetails: MovieDetails,
    onBackButtonClick: () -> Unit,
    onWishlistButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    with(movieDetails) {
        DetailsItem(
            modifier = modifier,
            id = id,
            type = "movie",
            title = title,
            overview = overview,
            posterPath = posterPath,
            releaseDate = releaseDate,
            runtime = if (runtime == NoMovieRuntimeValue) {
                stringResource(Res.string.no_runtime)
            } else {
                stringResource(Res.string.details_runtime_text, runtime.toString())
            },
            genres = genres.asNames(),
            voteAverage = 0.0,
            credits = credits,
            trailerKey = trailerKey,
            isWishlisted = isWishlisted,
            onBackButtonClick = onBackButtonClick,
            onWishlistButtonClick = onWishlistButtonClick
        )
    }
}

@Composable
internal fun MovieDetailsItemPlaceholder(
    onBackButtonClick: () -> Unit,
    onWishlistButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DetailsItemPlaceholder(
        modifier = modifier,
        onBackButtonClick = onBackButtonClick,
        onWishlistButtonClick = onWishlistButtonClick
    )
}
