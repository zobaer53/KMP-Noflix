package com.zobaer53.zedmovies.ios

import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import com.zobaer53.zedmovies.di.sharedModules
import com.zobaer53.zedmovies.di.viewModelModule
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Starts Koin once with the TMDB [apiKey], viewModelModule and sharedModules (as zedMoviesApplication does on Android).
 * Called from Swift as `IosKoinKt.startKoinIos(apiKey:)`. Not named `initKoin`: Objective-C export turns names that
 * start with `init` into `doInit...`.
 */
fun startKoinIos(apiKey: String) {
    val iosAppModule = module {
        single<zedMoviesApiKeyProvider> {
            object : zedMoviesApiKeyProvider {
                override val apiKey = apiKey
            }
        }
    }
    startKoin { modules(listOf(iosAppModule, viewModelModule) + sharedModules) }
}
