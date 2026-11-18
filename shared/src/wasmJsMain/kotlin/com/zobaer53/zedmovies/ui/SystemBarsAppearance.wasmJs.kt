package com.zobaer53.zedmovies.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The browser draws no system bars; keeping the interface a no-op lets the shared UI compose.
private object NoSystemBarsAppearance : SystemBarsAppearance {
    override fun setIconsFor(color: Color) = Unit
}

@Composable
actual fun rememberSystemBarsAppearance(): SystemBarsAppearance = NoSystemBarsAppearance
