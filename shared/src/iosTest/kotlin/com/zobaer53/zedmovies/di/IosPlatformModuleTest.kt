@file:OptIn(ExperimentalForeignApi::class)

package com.zobaer53.zedmovies.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zobaer53.zedmovies.data.database.model.common.MediaType
import com.zobaer53.zedmovies.data.database.model.wishlist.WishlistEntity
import com.zobaer53.zedmovies.data.database.zedMoviesDatabase
import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import com.zobaer53.zedmovies.data.platform.DeviceLocaleProvider
import com.zobaer53.zedmovies.domain.repository.MovieRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID

/**
 * The iOS actuals on the simulator: the real [platformModule] (Room on the bundled SQLite driver and DataStore, both in
 * the app's Documents directory) wired into the shared graph. The files it creates are deleted around each test.
 */
class IosPlatformModuleTest {
    private val testKey = stringPreferencesKey("i1_test_key")

    private fun koin(): Koin = koinApplication {
        modules(
            listOf(
                module {
                    single<zedMoviesApiKeyProvider> {
                        object : zedMoviesApiKeyProvider {
                            override val apiKey: String = "test"
                        }
                    }
                }
            ) + dataModules + platformModule
        )
    }.koin

    private fun deleteDocumentsFiles() {
        val directory = documentsDirectory()
        val names = listOf(zedMoviesDatabase.zedMovies_DATABASE, PREFERENCES_FILE_NAME)
        // SQLite's journal files sit next to the database.
        val suffixes = listOf("", "-wal", "-shm", "-journal", ".tmp")
        names.forEach { name ->
            suffixes.forEach { suffix -> NSFileManager.defaultManager.removeItemAtPath("$directory/$name$suffix", null) }
        }
    }

    @BeforeTest
    fun cleanBefore() = deleteDocumentsFiles()

    @AfterTest
    fun cleanAfter() = deleteDocumentsFiles()

    @Test
    fun graphResolvesTheShapedDependencies() {
        val koin = koin()
        try {
            koin.get<zedMoviesDatabase>()
            koin.get<DataStore<Preferences>>()
            koin.get<MovieRepository>()
            val language = koin.get<DeviceLocaleProvider>().language()
            assertTrue(language.isNotEmpty(), "the device language must not be empty")
        } finally {
            koin.get<zedMoviesDatabase>().close()
            koin.close()
        }
    }

    @Test
    fun wishlistSurvivesInTheBundledSqlite() = runTest {
        val koin = koin()
        val database = koin.get<zedMoviesDatabase>()
        try {
            val dao = database.wishlistDao
            dao.insert(WishlistEntity(networkId = 550, mediaType = MediaType.Wishlist.Movie))
            dao.insert(WishlistEntity(networkId = 1399, mediaType = MediaType.Wishlist.TvShow))

            assertEquals(listOf(550), dao.getByMediaType(MediaType.Wishlist.Movie).first().map { it.networkId })
            assertEquals(listOf(1399), dao.getByMediaType(MediaType.Wishlist.TvShow).first().map { it.networkId })
            assertTrue(dao.isWishlisted(MediaType.Wishlist.Movie, 550))
        } finally {
            database.close()
            koin.close()
        }
    }

    @Test
    fun wishlistIsStillThereInANewDatabaseInstance() = runTest {
        val first = koin()
        first.get<zedMoviesDatabase>().wishlistDao.insert(WishlistEntity(networkId = 680, mediaType = MediaType.Wishlist.Movie))
        first.get<zedMoviesDatabase>().close()
        first.close()

        val second = koin()
        try {
            val saved = second.get<zedMoviesDatabase>().wishlistDao.getByMediaType(MediaType.Wishlist.Movie).first()
            assertEquals(listOf(680), saved.map { it.networkId })
        } finally {
            second.get<zedMoviesDatabase>().close()
            second.close()
        }
    }

    @Test
    fun platformDataStoreReadsWhatItWrote() = runTest {
        val koin = koin()
        try {
            val store = koin.get<DataStore<Preferences>>()
            store.edit { it[testKey] = "de" }
            assertEquals("de", store.data.first()[testKey])
        } finally {
            koin.close()
        }
    }

    @Test
    fun preferencesPersistAcrossDataStoreInstances() = runTest {
        // A DataStore can't be closed, only its scope cancelled: cancelling it and creating a new one on the same path
        // is what a relaunch does.
        val path = (NSTemporaryDirectory() + "i1-" + NSUUID().UUIDString + ".preferences_pb").toPath()
        try {
            val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val first = PreferenceDataStoreFactory.createWithPath(scope = firstScope, produceFile = { path })
            first.edit { it[testKey] = "persisted" }
            firstScope.cancel()

            val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val second = PreferenceDataStoreFactory.createWithPath(scope = secondScope, produceFile = { path })
            assertEquals("persisted", second.data.first()[testKey])
            secondScope.cancel()
        } finally {
            NSFileManager.defaultManager.removeItemAtPath(path.toString(), null)
        }
    }
}
