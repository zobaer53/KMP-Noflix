package com.zobaer53.zedmovies.di

import com.zobaer53.zedmovies.BuildConfig
import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import org.koin.dsl.module

// What only the app can provide. The database, DataStore and locale/region providers come from
// :shared's platformModule (part of sharedModules).
val androidAppModule = module {
    single<zedMoviesApiKeyProvider> {
        object : zedMoviesApiKeyProvider {
            override val apiKey: String = BuildConfig.API_KEY
        }
    }
}
