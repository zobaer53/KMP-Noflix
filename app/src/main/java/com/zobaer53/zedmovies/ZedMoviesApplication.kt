
package com.zobaer53.zedmovies

import android.app.Application
import com.zobaer53.zedmovies.di.androidModule
import com.zobaer53.zedmovies.di.dataModules
import com.zobaer53.zedmovies.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class zedMoviesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@zedMoviesApplication)
            modules(listOf(androidModule, viewModelModule) + dataModules)
        }
    }
}
