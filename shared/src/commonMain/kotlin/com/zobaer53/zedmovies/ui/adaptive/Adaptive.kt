
package com.zobaer53.zedmovies.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/**
 * The kind of device the shared UI is rendered on. Hosts provide the value they know (a TV or a
 * desktop shell), otherwise it is derived from the window width. Phone is the default so existing
 * hosts (and the screenshot tests) keep their current layout without any changes.
 */
enum class FormFactor {
    Phone,
    Tablet,
    Tv,
    Desktop,
    Web
}

/**
 * How the user drives the UI. Touch keeps every existing interaction unchanged. Directional is a
 * D-pad / keyboard (TV, an Android tablet with a keyboard); Pointer is a mouse (desktop, web).
 */
enum class InputMode {
    Touch,
    Directional,
    Pointer
}

val LocalFormFactor = staticCompositionLocalOf { FormFactor.Phone }

val LocalInputMode = staticCompositionLocalOf { InputMode.Touch }

/**
 * True for layouts that have enough room for a navigation rail, a side panel and multi-column
 * feeds. Phone stays on the compact bottom-bar layout, which keeps the screenshots unchanged.
 */
fun FormFactor.isWide(): Boolean = this != FormFactor.Phone

/**
 * Derives the form factor from the host override, or from the window width when the host does not
 * know better. The thresholds follow the usual compact / medium / expanded window-size classes.
 */
@Composable
fun rememberDetectedFormFactor(override: FormFactor?): FormFactor {
    if (override != null) return override
    val density = LocalDensity.current
    val width = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    return when {
        width >= ExpandedWindowWidth -> FormFactor.Desktop
        width >= MediumWindowWidth -> FormFactor.Tablet
        else -> FormFactor.Phone
    }
}

/** Defaults to touch on phones/tablets and to pointer on hosts that are known to use a mouse. */
fun inputModeFor(formFactor: FormFactor): InputMode = when (formFactor) {
    FormFactor.Desktop, FormFactor.Web -> InputMode.Pointer
    FormFactor.Tv -> InputMode.Directional
    FormFactor.Phone, FormFactor.Tablet -> InputMode.Touch
}

private val MediumWindowWidth = 600.dp
private val ExpandedWindowWidth = 840.dp
