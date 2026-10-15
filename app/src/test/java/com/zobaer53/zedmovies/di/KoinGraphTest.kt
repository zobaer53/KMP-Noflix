package com.zobaer53.zedmovies.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import org.junit.Test
import org.koin.dsl.module
import org.koin.test.verify.verify

class KoinGraphTest {
    /** Every constructor parameter of every definition has a definition (Hilt checked this at compile time). */
    @Test
    fun `graph is complete`() {
        module {
            includes(androidModule, viewModelModule)
            includes(dataModules)
        }.verify(extraTypes = listOf(Context::class, SavedStateHandle::class, CreationExtras::class))
    }
}
