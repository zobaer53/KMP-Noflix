

package com.zobaer53.zedmovies.ui.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zobaer53.zedmovies.ui.designsystem.component.zedMoviesPlaceholder
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.no_overview
import com.zobaer53.zedmovies.resources.overview
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Overview(
    overview: String,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false
) {
    Column(
        modifier = modifier
            .padding(horizontal = zedMoviesTheme.spacing.extraMedium)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(zedMoviesTheme.spacing.small)
    ) {
        Text(
            text = stringResource(Res.string.overview),
            style = zedMoviesTheme.typography.semiBold.h4,
            color = zedMoviesTheme.colors.textPrimary
        )
        Text(
            modifier = if (isPlaceholder) {
                Modifier
                    .fillMaxWidth()
                    .zedMoviesPlaceholder(color = zedMoviesTheme.colors.textSecondary)
            } else {
                Modifier
            },
            text = overview.ifEmpty { stringResource(Res.string.no_overview) },
            style = zedMoviesTheme.typography.regular.h5,
            color = zedMoviesTheme.colors.textPrimaryVariant
        )
    }
}

@Composable
internal fun OverviewPlaceholder(modifier: Modifier = Modifier) {
    Overview(modifier = modifier, overview = PlaceholderText, isPlaceholder = true)
}

private const val PlaceholderText = ""
