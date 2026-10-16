package com.zobaer53.zedmovies.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.zobaer53.zedmovies.data.database.zedMoviesDatabase
import com.zobaer53.zedmovies.data.platform.AndroidDeviceLocaleProvider
import com.zobaer53.zedmovies.data.platform.AndroidRegionProvider
import com.zobaer53.zedmovies.data.platform.DeviceLocaleProvider
import com.zobaer53.zedmovies.data.platform.RegionProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    // Same database file as before (<app data>/databases/zedMovies.db, schema v3): users must not lose
    // their wish list. No setDriver: Room keeps the SupportSQLiteOpenHelper path (the driver is A5).
    single {
        Room.databaseBuilder(androidContext(), zedMoviesDatabase::class.java, zedMoviesDatabase.zedMovies_DATABASE)
            .build()
    }
    // What the former preferencesDataStore(name = "preferences") delegate did, as a single:
    // files/datastore/preferences.preferences_pb. Two DataStores on one file would throw.
    single<DataStore<Preferences>> {
        val context = androidContext()
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile(PREFERENCES_NAME) })
    }
    single<DeviceLocaleProvider> { AndroidDeviceLocaleProvider(androidContext()) }
    single<RegionProvider> { AndroidRegionProvider() }
}

private const val PREFERENCES_NAME = "preferences"
