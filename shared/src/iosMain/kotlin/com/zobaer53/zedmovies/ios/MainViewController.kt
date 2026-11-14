package com.zobaer53.zedmovies.ios

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.zobaer53.zedmovies.ui.configureStatusBar
import com.zobaer53.zedmovies.ui.playback.LocalMediaLauncher
import com.zobaer53.zedmovies.ui.zedMoviesApp
import platform.UIKit.UIViewController

/** The Compose UI as a view controller. Called from Swift as `MainViewControllerKt.MainViewController(host:)`. */
fun MainViewController(host: IosPlaybackHost): UIViewController =
    ComposeUIViewController(configure = { configureStatusBar() }) {
        val launcher = remember(host) { IosMediaLauncher(host) }
        CompositionLocalProvider(LocalMediaLauncher provides launcher) { zedMoviesApp() }
    }
