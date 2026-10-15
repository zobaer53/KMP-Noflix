package com.zobaer53.zedmovies.data.database.util

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.zobaer53.zedmovies.data.database.zedMoviesDatabase

class zedMoviesDatabaseTransactionProvider(
    private val zedMoviesDatabase: zedMoviesDatabase
) {
    // The multiplatform form of room-ktx's withTransaction: DAO calls made inside the block join the
    // transaction, because Room binds the writer connection to the coroutine.
    suspend fun <R> runWithTransaction(block: suspend () -> R): R =
        zedMoviesDatabase.useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
}
