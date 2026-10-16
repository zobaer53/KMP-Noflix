package com.zobaer53.zedmovies.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId as testTagsAsResourceIdProperty

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.testTagsAsResourceId(): Modifier = semantics { testTagsAsResourceIdProperty = true }

@Composable
internal actual fun cancelLabel(): String = stringResource(android.R.string.cancel)
