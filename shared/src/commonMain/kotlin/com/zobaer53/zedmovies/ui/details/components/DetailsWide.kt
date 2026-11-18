
package com.zobaer53.zedmovies.ui.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.zobaer53.zedmovies.data.model.Credits
import com.zobaer53.zedmovies.data.model.ReleaseDate
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.ic_calendar
import com.zobaer53.zedmovies.resources.ic_clock
import com.zobaer53.zedmovies.resources.ic_film
import com.zobaer53.zedmovies.resources.no_genre
import com.zobaer53.zedmovies.resources.no_release_date
import com.zobaer53.zedmovies.ui.adaptive.InputMode
import com.zobaer53.zedmovies.ui.adaptive.LocalInputMode
import com.zobaer53.zedmovies.ui.designsystem.component.ZedMoviesCardNetworkImage
import com.zobaer53.zedmovies.ui.designsystem.component.ZedMoviesNetworkImage
import com.zobaer53.zedmovies.ui.designsystem.component.zedMoviesOverlay
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme
import com.zobaer53.zedmovies.ui.smallcomponent.RatingItem
import com.zobaer53.zedmovies.ui.smallcomponent.zedMoviesBackButton
import com.zobaer53.zedmovies.ui.smallcomponent.zedMoviesWishlistButton
import org.jetbrains.compose.resources.stringResource

/**
 * Two-pane details layout for tablets, TV, desktop and web: the backdrop fills the left pane and
 * the scrollable detail panel sits on the right. Play takes initial focus on keyboard/D-pad hosts.
 */
@Suppress("LongParameterList")
@Composable
internal fun DetailsItemWide(
    type: String,
    title: String,
    overview: String,
    posterPath: String?,
    releaseDate: ReleaseDate,
    runtime: String,
    genres: List<String>,
    voteAverage: Double,
    credits: Credits,
    trailerKey: String?,
    isWishlisted: Boolean,
    onBackButtonClick: () -> Unit,
    onWishlistButtonClick: () -> Unit,
    id: Int,
    modifier: Modifier = Modifier
) {
    val inputMode = LocalInputMode.current
    val playFocusRequester = remember { FocusRequester() }
    LaunchedEffect(inputMode) {
        if (inputMode != InputMode.Touch) playFocusRequester.requestFocus()
    }

    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(BackdropWeight)
                .fillMaxHeight()
        ) {
            ZedMoviesNetworkImage(
                modifier = Modifier.fillMaxSize(),
                model = posterPath,
                contentDescription = title
            )
            zedMoviesOverlay(
                color = zedMoviesTheme.colors.primary,
                alpha = BackdropAlpha,
                modifier = Modifier.fillMaxSize()
            )
            zedMoviesBackButton(
                onClick = onBackButtonClick,
                modifier = Modifier.padding(zedMoviesTheme.spacing.extraMedium)
            )
        }
        Column(
            modifier = Modifier
                .weight(PanelWeight)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = zedMoviesTheme.spacing.extraMedium),
            verticalArrangement = Arrangement.spacedBy(zedMoviesTheme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = zedMoviesTheme.typography.semiBold.h2,
                    color = zedMoviesTheme.colors.textPrimary
                )
                zedMoviesWishlistButton(
                    isWishlisted = isWishlisted,
                    onClick = onWishlistButtonClick
                )
            }
            ZedMoviesCardNetworkImage(
                modifier = Modifier.size(width = WidePosterWidth, height = WidePosterHeight),
                model = posterPath,
                contentDescription = title,
                shape = zedMoviesTheme.shapes.smallMedium
            )
            IconAndText(
                iconResourceId = Res.drawable.ic_calendar,
                text = releaseDate.year.ifEmpty { stringResource(Res.string.no_release_date) }
            )
            IconAndText(iconResourceId = Res.drawable.ic_clock, text = runtime)
            IconAndText(
                iconResourceId = Res.drawable.ic_film,
                text = genres.joinToString(separator = GenreSeparator)
                    .ifEmpty { stringResource(Res.string.no_genre) }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(zedMoviesTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RatingItem(
                    type = type,
                    title = title,
                    releaseDate = releaseDate.year,
                    rating = voteAverage,
                    tmdbId = id,
                    playFocusRequester = playFocusRequester
                )
                if (trailerKey != null) TrailerItem(trailerKey = trailerKey)
            }
            Overview(overview = overview)
            CastAndCrew(credits = credits)
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
        }
    }
}

private const val BackdropWeight = 1.1f
private const val PanelWeight = 1f
private const val BackdropAlpha = 0.2f
private const val GenreSeparator = ", "
private val WidePosterWidth = 160.dp
private val WidePosterHeight = 224.dp
