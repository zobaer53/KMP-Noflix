package com.zobaer53.zedmovies.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import com.zobaer53.zedmovies.di.sharedModules
import com.zobaer53.zedmovies.di.viewModelModule
import kotlin.test.Test
import org.koin.dsl.module
import org.koin.test.verify.verify

class ViewModelGraphTest {
    /** Every ViewModel's use cases resolve from :shared's graph (KoinGraphTest checks the same on Android). */
    @Test
    fun viewModelsResolveFromTheSharedGraph() {
        module {
            single<zedMoviesApiKeyProvider> {
                object : zedMoviesApiKeyProvider {
                    override val apiKey: String = "test"
                }
            }
            includes(viewModelModule)
            includes(sharedModules)
            // Navigation supplies these per back-stack entry (nav arguments, Search's saved filters).
        }.verify(extraTypes = listOf(SavedStateHandle::class, CreationExtras::class))
    }
}
