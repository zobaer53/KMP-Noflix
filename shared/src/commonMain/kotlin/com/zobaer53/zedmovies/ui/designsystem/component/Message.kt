

package com.zobaer53.zedmovies.ui.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun zedMoviesMessage(
    messageResourceId: StringResource,
    imageResourceId: DrawableResource,
    modifier: Modifier = Modifier
) {
    zedMoviesCenteredBox(modifier = modifier.padding(horizontal = zedMoviesTheme.spacing.largest)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(zedMoviesTheme.spacing.medium)
        ) {
            Image(
                painter = painterResource(imageResourceId),
                contentDescription = stringResource(messageResourceId)
            )
            Text(
                text = stringResource(messageResourceId),
                style = zedMoviesTheme.typography.medium.h3,
                color = zedMoviesTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}
