
package com.zobaer53.zedmovies.ui.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import com.zobaer53.zedmovies.ui.adaptive.FormFactor
import com.zobaer53.zedmovies.ui.adaptive.LocalFormFactor

/**
 * Remembers which child of a lazy list had focus and restores it when the list is revisited (for
 * example after navigating to details and pressing Back). Phone layouts keep the plain list so the
 * existing screenshots and touch interactions are untouched.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.zedMoviesFocusRestorer(): Modifier =
    if (LocalFormFactor.current == FormFactor.Phone) this else focusRestorer()
