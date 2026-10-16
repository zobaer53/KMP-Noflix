package com.zobaer53.zedmovies.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

/**
 * The system bars are transparent (edge-to-edge), so only their icon color is chosen:
 * dark icons over light content, light icons over dark content.
 */
@Stable
interface SystemBarsAppearance {
    fun setIconsFor(color: Color)
}

// Imperative (not a composable that takes the color) so the icons flip exactly when
// zedMoviesAppState.setSystemBarsColor is called, e.g. during the Details transitions.
@Composable
expect fun rememberSystemBarsAppearance(): SystemBarsAppearance
