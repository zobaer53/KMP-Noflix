package com.zobaer53.zedmovies.di

import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import kotlin.test.Test
import org.koin.dsl.module
import org.koin.test.verify.verify

class SharedKoinGraphTest {
    /** :shared's graph is complete once the app adds the API key (its only outside input). */
    @Test
    fun graphIsCompleteWithTheApiKey() {
        module {
            single<zedMoviesApiKeyProvider> {
                object : zedMoviesApiKeyProvider {
                    override val apiKey: String = "test"
                }
            }
            includes(sharedModules)
        }.verify()
    }
}
