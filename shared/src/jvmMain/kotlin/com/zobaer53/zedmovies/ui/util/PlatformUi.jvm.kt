package com.zobaer53.zedmovies.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Resource ids are an Android accessibility concept: nothing to expose on the desktop.
internal actual fun Modifier.testTagsAsResourceId(): Modifier = this

@Composable
internal actual fun cancelLabel(): String = "Cancel"
