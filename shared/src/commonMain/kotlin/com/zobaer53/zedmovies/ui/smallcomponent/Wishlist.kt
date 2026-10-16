
package com.zobaer53.zedmovies.ui.smallcomponent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.ic_wishlist
import com.zobaer53.zedmovies.resources.wishlist
import com.zobaer53.zedmovies.ui.designsystem.component.zedMoviesIconButton
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun zedMoviesWishlistButton(
    isWishlisted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    zedMoviesIconButton(
        modifier = modifier
            .size(zedMoviesWishlistButtonShapeSize)
            .background(
                color = zedMoviesTheme.colors.primaryVariant,
                shape = zedMoviesTheme.shapes.smallMedium
            ),
        iconResourceId = Res.drawable.ic_wishlist,
        contentDescription = stringResource(Res.string.wishlist),
        onClick = onClick,
        tint = zedMoviesTheme.colors.error.let { color ->
            if (isWishlisted) color else color.copy(alpha = DisabledContentAlpha)
        }
    )
}

private val zedMoviesWishlistButtonShapeSize = 32.dp
private const val DisabledContentAlpha = 0.5f
