package com.zobaer53.zedmovies.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.zobaer53.zedmovies.data.database.zedMoviesDatabase
import com.zobaer53.zedmovies.data.platform.DeviceLocaleProvider
import com.zobaer53.zedmovies.data.platform.JvmDeviceLocaleProvider
import com.zobaer53.zedmovies.data.platform.JvmRegionProvider
import com.zobaer53.zedmovies.data.platform.RegionProvider
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

// The JVM graph (host tests only): an in-memory database and a DataStore in a fresh temp folder.
actual val platformModule: Module = module {
    single {
        Room.inMemoryDatabaseBuilder<zedMoviesDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
    single<DataStore<Preferences>> {
        val folder = Files.createTempDirectory("zedmovies").toFile()
        PreferenceDataStoreFactory.create(produceFile = { folder.resolve("preferences.preferences_pb") })
    }
    single<DeviceLocaleProvider> { JvmDeviceLocaleProvider() }
    single<RegionProvider> { JvmRegionProvider() }
}
