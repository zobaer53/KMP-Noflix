package com.zobaer53.zedmovies.ui.designsystem.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

@Composable
fun zedMoviesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val montserrat = rememberMontserrat()
    val typography = remember(montserrat) { zedMoviesTypography(montserrat) }
    val materialTypography = remember(typography) { materialTypography(typography) }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        shapes = Shapes,
        typography = materialTypography
    ) { ProvidezedMoviesThemeDependencies(typography = typography, content = content) }
}

@Composable
private fun ProvidezedMoviesThemeDependencies(
    typography: zedMoviesTypography,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalzedMoviesColors provides if (darkTheme) DarkzedMoviesColors else LightzedMoviesColors,
        LocalzedMoviesShapes provides zedMoviesShapes(),
        LocalzedMoviesTypography provides typography,
        LocalzedMoviesSpacing provides zedMoviesSpacing()
    ) { ProvideMaterialThemeDependencies(content = content) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProvideMaterialThemeDependencies(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalIndication provides rememberzedMoviesRipple(),
        LocalRippleConfiguration provides zedMoviesRippleConfiguration()
    ) {
        ProvideTextStyle(value = zedMoviesTheme.typography.regular.h4, content = content)
    }
}

object zedMoviesTheme {
    val colors: zedMoviesColors
        @Composable
        @ReadOnlyComposable
        get() = LocalzedMoviesColors.current

    val shapes: zedMoviesShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalzedMoviesShapes.current

    val typography: zedMoviesTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalzedMoviesTypography.current

    val spacing: zedMoviesSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalzedMoviesSpacing.current
}
