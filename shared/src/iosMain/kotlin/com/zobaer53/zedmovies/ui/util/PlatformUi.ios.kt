package com.zobaer53.zedmovies.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import platform.Foundation.NSBundle

// Compose on iOS already exposes testTag as the accessibility identifier, which is what Maestro's `id:` uses.
internal actual fun Modifier.testTagsAsResourceId(): Modifier = this

// UIKit's own localized "Cancel" (what Android's android.R.string.cancel gives), "Cancel" if the bundle lacks it.
private val uiKitCancelLabel: String by lazy {
    NSBundle.bundleWithIdentifier("com.apple.UIKit")?.localizedStringForKey("Cancel", "Cancel", null) ?: "Cancel"
}

@Composable
internal actual fun cancelLabel(): String = uiKitCancelLabel
