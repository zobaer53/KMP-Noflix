
package com.zobaer53.zedmovies.ui.details.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zobaer53.zedmovies.data.model.TvShowDetails
import com.zobaer53.zedmovies.ui.smallcomponent.mapper.NoTvShowRuntimeValue
import com.zobaer53.zedmovies.ui.smallcomponent.mapper.asNames
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.details_runtime_text
import com.zobaer53.zedmovies.resources.no_runtime
import org.jetbrains.compose.resources.stringResource

@Suppress("ReusedModifierInstance")
@Composable
internal fun TvShowDetailsItem(
    tvShowDetails: TvShowDetails,
    onBackButtonClick: () -> Unit,
    onWishlistButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    with(tvShowDetails) {
        DetailsItem(
            modifier = modifier,
            id = id,
            type = "tv",
            title = name,
            overview = overview,
            posterPath = posterPath,
            releaseDate = firstAirDate,
            runtime = if (episodeRunTime == NoTvShowRuntimeValue) {
                stringResource(Res.string.no_runtime)
            } else {
                stringResource(Res.string.details_runtime_text, episodeRunTime.toString())
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
internal fun TvShowDetailsItemPlaceholder(
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
