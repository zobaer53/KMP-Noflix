package com.zobaer53.zedmovies.ui.details.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zobaer53.zedmovies.R
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.ic_film
import com.zobaer53.zedmovies.ui.designsystem.component.zedMoviesIcon
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme
import com.zobaer53.zedmovies.ui.playback.LocalMediaLauncher

/** Opens the YouTube trailer; styled like the Play chip ([RatingItem][com.zobaer53.zedmovies.ui.smallcomponent.RatingItem]). */
@Composable
internal fun TrailerItem(
    trailerKey: String,
    modifier: Modifier = Modifier
) {
    val mediaLauncher = LocalMediaLauncher.current
    // Not Card(onClick): that enforces a 48dp touch target and would sit taller than the Play chip.
    Card(
        modifier = modifier
            .clip(zedMoviesTheme.shapes.small)
            .clickable { mediaLauncher.playTrailer(trailerKey) },
        shape = zedMoviesTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = zedMoviesTheme.colors.mediaScrim,
            contentColor = zedMoviesTheme.colors.textOnMedia
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = zedMoviesTheme.spacing.small,
                vertical = zedMoviesTheme.spacing.extraSmall
            )
        ) {
            zedMoviesIcon(
                modifier = Modifier.size(TrailerIconSize),
                iconResourceId = Res.drawable.ic_film,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(zedMoviesTheme.spacing.extraSmall))
            Text(
                text = stringResource(id = R.string.trailer),
                style = zedMoviesTheme.typography.semiBold.h6
            )
        }
    }
}

private val TrailerIconSize = 16.dp
