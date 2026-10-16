package com.zobaer53.zedmovies.data.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/** A fresh in-memory database with the bundled SQLite (JVM tests only; never in the APK). */
internal fun inMemoryDatabase(): zedMoviesDatabase =
    Room.inMemoryDatabaseBuilder<zedMoviesDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
