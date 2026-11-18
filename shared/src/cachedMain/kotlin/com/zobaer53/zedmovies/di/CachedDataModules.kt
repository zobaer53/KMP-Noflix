package com.zobaer53.zedmovies.di

import com.zobaer53.zedmovies.data.database.source.MovieDatabaseDataSource
import com.zobaer53.zedmovies.data.database.source.MovieDetailsDatabaseDataSource
import com.zobaer53.zedmovies.data.database.source.TvShowDatabaseDataSource
import com.zobaer53.zedmovies.data.database.source.TvShowDetailsDatabaseDataSource
import com.zobaer53.zedmovies.data.database.source.WishlistDatabaseDataSource
import com.zobaer53.zedmovies.data.database.util.zedMoviesDatabaseTransactionProvider
import com.zobaer53.zedmovies.data.database.zedMoviesDatabase
import com.zobaer53.zedmovies.data.datastore.PreferencesDataStoreDataSource
import com.zobaer53.zedmovies.data.repository.FilterOptionsRepositoryImpl
import com.zobaer53.zedmovies.data.repository.MovieDetailsRepositoryImpl
import com.zobaer53.zedmovies.data.repository.MovieRepositoryImpl
import com.zobaer53.zedmovies.data.repository.TvShowDetailsRepositoryImpl
import com.zobaer53.zedmovies.data.repository.TvShowRepositoryImpl
import com.zobaer53.zedmovies.data.repository.WishlistRepositoryImpl
import com.zobaer53.zedmovies.domain.repository.FilterOptionsRepository
import com.zobaer53.zedmovies.domain.repository.MovieDetailsRepository
import com.zobaer53.zedmovies.domain.repository.MovieRepository
import com.zobaer53.zedmovies.domain.repository.TvShowDetailsRepository
import com.zobaer53.zedmovies.domain.repository.TvShowRepository
import com.zobaer53.zedmovies.domain.repository.WishlistRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val cachedDataModules: List<Module> = listOf(
    module {
        factory { get<zedMoviesDatabase>().movieDao }
        factory { get<zedMoviesDatabase>().tvShowDao }
        factory { get<zedMoviesDatabase>().movieRemoteKeyDao }
        factory { get<zedMoviesDatabase>().tvShowRemoteKeyDao }
        factory { get<zedMoviesDatabase>().movieDetailsDao }
        factory { get<zedMoviesDatabase>().tvShowDetailsDao }
        factory { get<zedMoviesDatabase>().wishlistDao }
        factoryOf(::zedMoviesDatabaseTransactionProvider)
    },
    module {
        factoryOf(::MovieDatabaseDataSource)
        factoryOf(::TvShowDatabaseDataSource)
        factoryOf(::MovieDetailsDatabaseDataSource)
        factoryOf(::TvShowDetailsDatabaseDataSource)
        factoryOf(::WishlistDatabaseDataSource)
        factoryOf(::PreferencesDataStoreDataSource)
    },
    module {
        factoryOf(::MovieRepositoryImpl) bind MovieRepository::class
        factoryOf(::TvShowRepositoryImpl) bind TvShowRepository::class
        factoryOf(::MovieDetailsRepositoryImpl) bind MovieDetailsRepository::class
        factoryOf(::TvShowDetailsRepositoryImpl) bind TvShowDetailsRepository::class
        factoryOf(::WishlistRepositoryImpl) bind WishlistRepository::class
        singleOf(::FilterOptionsRepositoryImpl) bind FilterOptionsRepository::class
    }
)
