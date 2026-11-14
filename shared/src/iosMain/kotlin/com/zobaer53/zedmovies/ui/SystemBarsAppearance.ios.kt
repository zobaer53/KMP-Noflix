package com.zobaer53.zedmovies.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration

private object IosSystemBarsAppearance : SystemBarsAppearance {
    override fun setIconsFor(color: Color) = Unit
}

@Composable
actual fun rememberSystemBarsAppearance(): SystemBarsAppearance = IosSystemBarsAppearance

/** Called from MainViewController's `configure`. A no-op for now (the default status bar style follows the system theme). */
internal fun ComposeUIViewControllerConfiguration.configureStatusBar() {}
