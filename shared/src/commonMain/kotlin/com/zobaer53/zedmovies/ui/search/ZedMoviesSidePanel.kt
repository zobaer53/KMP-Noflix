
package com.zobaer53.zedmovies.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme

/**
 * The wide-screen counterpart of the filter bottom sheet: a dismissible scrim with the filters in
 * an end-aligned panel. Only built from common Compose APIs, so it works on every host.
 */
@Composable
internal fun ZedMoviesSidePanel(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .background(color = Color.Black.copy(alpha = ScrimAlpha))
                    .clickable(onClick = onDismiss)
            )
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(SidePanelWidth),
                color = zedMoviesTheme.colors.primary,
                contentColor = zedMoviesTheme.colors.textPrimary,
                content = content
            )
        }
    }
}

private val SidePanelWidth = 420.dp
private const val ScrimAlpha = 0.6f
