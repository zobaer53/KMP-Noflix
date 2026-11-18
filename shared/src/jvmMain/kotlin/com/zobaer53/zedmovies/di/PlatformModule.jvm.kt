
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
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

// The JVM target is used by host tests (in memory) and by the Compose Desktop app (persistent).
// The desktop shell sets NOFLIX_DATA_DIR to the OS app-data folder; without it, tests keep the
// old fresh-temp behaviour so they cannot leak state into each other.
private fun dataDirectory(): File? = (System.getProperty("noflix.data.dir") ?: System.getenv("NOFLIX_DATA_DIR"))
    ?.takeIf { it.isNotBlank() }
    ?.let { File(it).apply { mkdirs() } }

actual val platformModule: Module = module {
    single {
        val folder = dataDirectory()
        if (folder == null) {
            Room.inMemoryDatabaseBuilder<zedMoviesDatabase>()
        } else {
            Room.databaseBuilder<zedMoviesDatabase>(
                name = File(folder, "zedMovies.db").absolutePath
            )
        }
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
    single<DataStore<Preferences>> {
        val folder = dataDirectory()
        val dir = folder?.resolve("datastore")?.apply { mkdirs() }
            ?: Files.createTempDirectory("zedmovies").toFile()
        PreferenceDataStoreFactory.create(produceFile = { File(dir, "preferences.preferences_pb") })
    }
    single<DeviceLocaleProvider> { JvmDeviceLocaleProvider() }
    single<RegionProvider> { JvmRegionProvider() }
}
