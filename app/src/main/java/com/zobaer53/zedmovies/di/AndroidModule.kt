package com.zobaer53.zedmovies.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.zobaer53.zedmovies.BuildConfig
import com.zobaer53.zedmovies.data.database.zedMoviesDatabase
import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import com.zobaer53.zedmovies.data.platform.DeviceLocaleProvider
import com.zobaer53.zedmovies.data.platform.RegionProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

// Everything that needs Android or the app's BuildConfig. Stays in :app.
val androidModule = module {
    // Same database file as before (zedMovies.db, schema v3): users must not lose their wish list.
    single {
        Room.databaseBuilder(androidContext(), zedMoviesDatabase::class.java, zedMoviesDatabase.zedMovies_DATABASE)
            .build()
    }
    // Same delegate and file as before: files/datastore/preferences.preferences_pb.
    single<DataStore<Preferences>> { androidContext().preferencesDataStore }
    single<zedMoviesApiKeyProvider> {
        object : zedMoviesApiKeyProvider {
            override val apiKey: String = BuildConfig.API_KEY
        }
    }
    single<DeviceLocaleProvider> { AndroidDeviceLocaleProvider(androidContext()) }
    single<RegionProvider> { AndroidRegionProvider() }
}

private const val PREFERENCES_NAME = "preferences"
private val Context.preferencesDataStore by preferencesDataStore(name = PREFERENCES_NAME)
