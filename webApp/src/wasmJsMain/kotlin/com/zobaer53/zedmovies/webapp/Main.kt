package com.zobaer53.zedmovies.webapp

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import com.zobaer53.zedmovies.di.sharedModules
import com.zobaer53.zedmovies.di.viewModelModule
import com.zobaer53.zedmovies.ui.adaptive.FormFactor
import com.zobaer53.zedmovies.ui.adaptive.InputMode
import com.zobaer53.zedmovies.ui.playback.LocalMediaLauncher
import com.zobaer53.zedmovies.ui.zedMoviesApp
import kotlinx.browser.document
import org.koin.core.context.startKoin
import org.koin.dsl.module

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(
            module {
                single<zedMoviesApiKeyProvider> {
                    object : zedMoviesApiKeyProvider {
                        override val apiKey: String? = WEB_API_KEY.ifBlank { null }
                    }
                }
            } + viewModelModule + sharedModules
        )
    }
    ComposeViewport(document.body!!) {
        CompositionLocalProvider(LocalMediaLauncher provides WebMediaLauncher) {
            zedMoviesApp(formFactor = FormFactor.Web, inputMode = InputMode.Pointer)
        }
    }
}
