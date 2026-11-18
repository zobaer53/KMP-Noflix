
package com.zobaer53.zedmovies.ui.designsystem.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zobaer53.zedmovies.navigation.TopLevelDestination
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The large-screen counterpart of [com.zobaer53.zedmovies.ui.component.zedMoviesBottomBar].
 * It is built only from shared Material3 components and the same [TopLevelDestination] items, so it
 * works on Android, desktop, web and iOS without any platform-specific code.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZedNavigationRail(
    destinations: Array<TopLevelDestination>,
    currentDestination: TopLevelDestination,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical)
        ),
        containerColor = zedMoviesTheme.colors.primary,
        contentColor = zedMoviesTheme.colors.textPrimary
    ) {
        destinations.forEach { destination ->
            val selected = destination == currentDestination
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.iconResourceId),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(destination.textResourceId)) },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = zedMoviesTheme.colors.accent,
                    selectedTextColor = zedMoviesTheme.colors.accent,
                    unselectedIconColor = zedMoviesTheme.colors.textSecondary,
                    unselectedTextColor = zedMoviesTheme.colors.textSecondary,
                    indicatorColor = zedMoviesTheme.colors.accentContainer
                )
            )
        }
    }
}
