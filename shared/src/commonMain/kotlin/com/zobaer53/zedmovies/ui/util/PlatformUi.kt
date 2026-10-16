package com.zobaer53.zedmovies.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// UI bits that only exist on one platform. The actuals are in PlatformUi.android.kt and PlatformUi.jvm.kt.

/** Exposes the test tags inside this node as resource ids (Maestro finds the sliders by them on Android). */
internal expect fun Modifier.testTagsAsResourceId(): Modifier

/** The platform's own "Cancel" label (on Android the framework string, in the device language). */
@Composable
internal expect fun cancelLabel(): String
