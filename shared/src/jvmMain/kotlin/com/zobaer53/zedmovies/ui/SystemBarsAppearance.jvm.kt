package com.zobaer53.zedmovies.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The JVM target only runs host tests: there are no system bars to color.
private object NoSystemBarsAppearance : SystemBarsAppearance {
    override fun setIconsFor(color: Color) = Unit
}

@Composable
actual fun rememberSystemBarsAppearance(): SystemBarsAppearance = NoSystemBarsAppearance
