package com.zobaer53.zedmovies.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.zobaer53.zedmovies.data.database.zedMoviesDatabase
import com.zobaer53.zedmovies.data.platform.DeviceLocaleProvider
import com.zobaer53.zedmovies.data.platform.IosDeviceLocaleProvider
import com.zobaer53.zedmovies.data.platform.IosRegionProvider
import com.zobaer53.zedmovies.data.platform.RegionProvider
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

// The app's Documents directory (created if missing): the same files as on Android, backed up with the device so
// the wish list survives a restore, and invisible in the Files app (no UIFileSharingEnabled in Info.plist).
// Internal so the iOS tests can find and delete their files.
@OptIn(ExperimentalForeignApi::class)
internal fun documentsDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    return requireNotNull(url?.path)
}

internal const val PREFERENCES_FILE_NAME = "preferences.preferences_pb"

actual val platformModule: Module = module {
    single {
        Room.databaseBuilder<zedMoviesDatabase>(name = documentsDirectory() + "/" + zedMoviesDatabase.zedMovies_DATABASE)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(produceFile = { (documentsDirectory() + "/" + PREFERENCES_FILE_NAME).toPath() })
    }
    single<DeviceLocaleProvider> { IosDeviceLocaleProvider() }
    single<RegionProvider> { IosRegionProvider() }
}
