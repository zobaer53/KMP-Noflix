package com.zobaer53.zedmovies.data.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.zobaer53.zedmovies.data.database.model.common.MediaType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule

/**
 * Users upgrade from the public release (schema v2 or v3) with their wish list on disk. This builds a
 * v2 database from the committed `schemas/…/2.json`, migrates it with the AutoMigration, checks the
 * result against `3.json`, then opens it the way the app does and reads the wish list back.
 */
class MigrationTest {
    private val databaseFile: Path = Files.createTempDirectory("migration").resolve(zedMoviesDatabase.zedMovies_DATABASE)

    @get:Rule
    val helper = MigrationTestHelper(
        schemaDirectoryPath = Path.of("schemas"), // the Gradle test task runs in shared/
        databasePath = databaseFile,
        driver = BundledSQLiteDriver(),
        databaseClass = zedMoviesDatabase::class,
        databaseFactory = { zedMoviesDatabaseConstructor.initialize() }
    )

    @Test
    fun wishlistSurvivesMigrationFrom2To3() = runTest {
        helper.createDatabase(2).apply {
            movieIds.forEach { execSQL("INSERT INTO wishlist (network_id, media_type) VALUES ($it, 'Movie')") }
            tvShowIds.forEach { execSQL("INSERT INTO wishlist (network_id, media_type) VALUES ($it, 'TvShow')") }
            close()
        }

        // Validates every table, column and index against 3.json.
        helper.runMigrationsAndValidate(3).close()

        val database = Room.databaseBuilder<zedMoviesDatabase>(name = databaseFile.toString())
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        try {
            val wishlist = database.wishlistDao
            assertEquals(movieIds, wishlist.getByMediaType(MediaType.Wishlist.Movie).first().map { it.networkId })
            assertEquals(tvShowIds, wishlist.getByMediaType(MediaType.Wishlist.TvShow).first().map { it.networkId })
            assertTrue(wishlist.isWishlisted(MediaType.Wishlist.TvShow, tvShowIds.last()))
        } finally {
            database.close()
        }
    }

    private companion object {
        val movieIds = listOf(550, 680, 13, 155, 27205, 603, 278)
        val tvShowIds = listOf(1399, 1396, 66732, 94605, 100088)
    }
}
