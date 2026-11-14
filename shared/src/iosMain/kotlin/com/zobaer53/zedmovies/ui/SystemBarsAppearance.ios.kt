package com.zobaer53.zedmovies.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.uikit.LocalUIViewController
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleDefault
import platform.UIKit.UIStatusBarStyleLightContent

// The status bar style the hosting view controller reports. UIKit reads it through the delegate below, so it
// only has to be set before setNeedsStatusBarAppearanceUpdate(). Main thread only, like all UI state.
// `Default` follows the system theme until the first screen picks a style.
private var statusBarStyle: UIStatusBarStyle = UIStatusBarStyleDefault

// ComposeUIViewControllerDelegate is deprecated in CMP 1.12.2 in favor of overriding the methods in a parent
// UIViewController (Swift, I2/I3's files). It still works, so this keeps the status bar in Kotlin.
@Suppress("DEPRECATION")
private object IosStatusBarDelegate : ComposeUIViewControllerDelegate {
    override val preferredStatusBarStyle: UIStatusBarStyle?
        get() = statusBarStyle
}

@Composable
actual fun rememberSystemBarsAppearance(): SystemBarsAppearance {
    val viewController = LocalUIViewController.current
    return remember(viewController) {
        object : SystemBarsAppearance {
            override fun setIconsFor(color: Color) {
                // Dark icons over light content, light icons over dark content (as WindowInsetsController does on Android).
                statusBarStyle = if (color.luminance() > 0.5f) UIStatusBarStyleDarkContent else UIStatusBarStyleLightContent
                viewController.setNeedsStatusBarAppearanceUpdate()
            }
        }
    }
}

/** Called from MainViewController's `configure`: lets [rememberSystemBarsAppearance] choose the status bar style. */
@Suppress("DEPRECATION")
internal fun ComposeUIViewControllerConfiguration.configureStatusBar() {
    delegate = IosStatusBarDelegate
}
