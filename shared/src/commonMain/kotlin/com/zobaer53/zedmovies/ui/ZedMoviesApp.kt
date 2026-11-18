
package com.zobaer53.zedmovies.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.zobaer53.zedmovies.navigation.ZedMoviesNavHost
import com.zobaer53.zedmovies.ui.adaptive.FormFactor
import com.zobaer53.zedmovies.ui.adaptive.InputMode
import com.zobaer53.zedmovies.ui.adaptive.LocalFormFactor
import com.zobaer53.zedmovies.ui.adaptive.LocalInputMode
import com.zobaer53.zedmovies.ui.adaptive.inputModeFor
import com.zobaer53.zedmovies.ui.adaptive.isWide
import com.zobaer53.zedmovies.ui.adaptive.rememberDetectedFormFactor
import com.zobaer53.zedmovies.ui.component.zedMoviesBottomBar
import com.zobaer53.zedmovies.ui.designsystem.component.LocalSnackbarHostState
import com.zobaer53.zedmovies.ui.designsystem.component.ZedNavigationRail
import com.zobaer53.zedmovies.ui.designsystem.component.zedMoviesSnackbarHost
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Suppress("ModifierMissing")
@Composable
fun zedMoviesApp(
    appState: zedMoviesAppState = rememberzedMoviesAppState(),
    /**
     * Hosts that know their device (TV, desktop, web) pass it here. When null the form factor is
     * derived from the window width. Phones keep the compact bottom-bar layout.
     */
    formFactor: FormFactor? = null,
    inputMode: InputMode? = null
) {
    zedMoviesTheme {
        val systemBarsColor = zedMoviesTheme.colors.primary
        LaunchedEffect(systemBarsColor) { appState.setSystemBarsColor(systemBarsColor) }

        val detectedFormFactor = rememberDetectedFormFactor(formFactor)
        val effectiveInputMode = inputMode ?: inputModeFor(detectedFormFactor)
        val showBottomBar = appState.shouldShowBottomBar

        CompositionLocalProvider(
            LocalFormFactor provides detectedFormFactor,
            LocalInputMode provides effectiveInputMode,
            LocalSnackbarHostState provides appState.snackbarHostState
        ) {
            val snackbarHost: @Composable () -> Unit = {
                zedMoviesSnackbarHost(
                    modifier = Modifier.windowInsetsPadding(
                        if (showBottomBar) {
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                        } else {
                            WindowInsets.safeDrawing
                        }
                    ),
                    snackbarHostState = appState.snackbarHostState
                )
            }
            val navHost: @Composable (Modifier) -> Unit = { modifier ->
                ZedMoviesNavHost(
                    modifier = modifier,
                    navController = appState.navController,
                    startDestination = appState.startDestination,
                    onNavigateToDestination = appState::navigate,
                    onBackClick = appState::onBackClick,
                    onShowMessage = { message -> appState.showMessage(message) },
                    onSetSystemBarsColorTransparent = { appState.setSystemBarsColor(Color.Transparent) },
                    onResetSystemBarsColor = { appState.setSystemBarsColor(systemBarsColor) }
                )
            }
            // Escape backs out on keyboard-driven hosts. Android's system Back is already handled
            // by the activity/nav controller; Esc is the equivalent for desktop, web and TV keyboards.
            val keyboardBackModifier = Modifier
                .focusable()
                .onKeyEvent { event ->
                    if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                        appState.onBackClick()
                        true
                    } else {
                        false
                    }
                }

            if (detectedFormFactor.isWide()) {
                Row(modifier = keyboardBackModifier.fillMaxSize()) {
                    ZedNavigationRail(
                        destinations = appState.topLevelDestinations,
                        currentDestination = appState.currentTopLevelDestination,
                        onNavigateToDestination = appState::navigate
                    )
                    Scaffold(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        snackbarHost = snackbarHost,
                        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
                    ) { innerPadding ->
                        navHost(
                            Modifier
                                .padding(paddingValues = innerPadding)
                                .consumeWindowInsets(paddingValues = innerPadding)
                        )
                    }
                }
            } else {
                Scaffold(
                    modifier = keyboardBackModifier,
                    bottomBar = {
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = BottomBarEnterTransition,
                            exit = BottomBarExitTransition
                        ) {
                            zedMoviesBottomBar(
                                destinations = appState.topLevelDestinations,
                                currentDestination = appState.currentTopLevelDestination,
                                onNavigateToDestination = appState::navigate
                            )
                        }
                    },
                    snackbarHost = snackbarHost,
                    contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
                ) { innerPadding ->
                    navHost(
                        Modifier
                            .padding(paddingValues = innerPadding)
                            .consumeWindowInsets(paddingValues = innerPadding)
                    )
                }
            }
        }
    }
}

private val BottomBarEnterTransition = fadeIn() + expandVertically(expandFrom = Alignment.Top)
private val BottomBarExitTransition = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
