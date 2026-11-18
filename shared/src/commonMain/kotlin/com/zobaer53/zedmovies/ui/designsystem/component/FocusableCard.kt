
package com.zobaer53.zedmovies.ui.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.zobaer53.zedmovies.ui.adaptive.InputMode
import com.zobaer53.zedmovies.ui.adaptive.LocalInputMode
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme

/**
 * A card that shows a focus border and a slight scale-up when reached with a D-pad or keyboard.
 * On touch screens it never draws the indicator, so phones keep their existing appearance.
 */
@Composable
fun FocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = zedMoviesTheme.shapes.smallMedium,
    containerColor: Color = zedMoviesTheme.colors.primaryVariant,
    content: @Composable BoxScope.() -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val showFocusIndicator = isFocused && LocalInputMode.current != InputMode.Touch

    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .focusable(enabled = enabled)
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                val scale = if (showFocusIndicator) FocusedScale else 1f
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (showFocusIndicator) {
                    Modifier.border(
                        width = FocusBorderWidth,
                        color = zedMoviesTheme.colors.accent,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

private const val FocusedScale = 1.03f
private val FocusBorderWidth = 2.dp
