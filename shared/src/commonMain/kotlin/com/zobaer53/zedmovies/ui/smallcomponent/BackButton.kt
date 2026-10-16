

package com.zobaer53.zedmovies.ui.smallcomponent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.back
import com.zobaer53.zedmovies.resources.ic_arrow_back
import com.zobaer53.zedmovies.ui.designsystem.component.zedMoviesIconButton
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun zedMoviesBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = zedMoviesTheme.colors.textPrimary
) {
    zedMoviesIconButton(
        modifier = modifier
            .size(zedMoviesBackButtonShapeSize)
            .background(
                color = zedMoviesTheme.colors.primaryVariant,
                shape = zedMoviesTheme.shapes.smallMedium
            ),
        iconResourceId = Res.drawable.ic_arrow_back,
        contentDescription = stringResource(Res.string.back),
        onClick = onClick,
        tint = tint
    )
}

private val zedMoviesBackButtonShapeSize = 32.dp
