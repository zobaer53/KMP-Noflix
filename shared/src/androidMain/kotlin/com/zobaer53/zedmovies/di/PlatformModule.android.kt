package com.zobaer53.zedmovies.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.zobaer53.zedmovies.data.database.zedMoviesDatabase
import com.zobaer53.zedmovies.data.platform.AndroidDeviceLocaleProvider
import com.zobaer53.zedmovies.data.platform.AndroidRegionProvider
import com.zobaer53.zedmovies.data.platform.DeviceLocaleProvider
import com.zobaer53.zedmovies.data.platform.RegionProvider
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    // Same database file as before (<app data>/databases/zedMovies.db, schema v3): users must not lose
    // their wish list. AndroidSQLiteDriver keeps Android's own SQLite (no extra native library, so the
    // 16 KB check is unaffected); the driver opens the file by its absolute path.
    single {
        val context = androidContext()
        Room.databaseBuilder<zedMoviesDatabase>(
            context = context.applicationContext,
            name = context.getDatabasePath(zedMoviesDatabase.zedMovies_DATABASE).absolutePath
        )
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
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
