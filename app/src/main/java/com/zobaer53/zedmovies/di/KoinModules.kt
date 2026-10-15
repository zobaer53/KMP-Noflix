package com.zobaer53.zedmovies.di

import com.zobaer53.zedmovies.data.database.source.MovieDatabaseDataSource
import com.zobaer53.zedmovies.data.database.source.MovieDetailsDatabaseDataSource
import com.zobaer53.zedmovies.data.database.source.TvShowDatabaseDataSource
import com.zobaer53.zedmovies.data.database.source.TvShowDetailsDatabaseDataSource
import com.zobaer53.zedmovies.data.database.source.WishlistDatabaseDataSource
import com.zobaer53.zedmovies.data.database.util.zedMoviesDatabaseTransactionProvider
import com.zobaer53.zedmovies.data.database.zedMoviesDatabase
import com.zobaer53.zedmovies.data.datastore.PreferencesDataStoreDataSource
import com.zobaer53.zedmovies.data.network.api.zedMoviesApi
import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import com.zobaer53.zedmovies.data.network.source.FilterOptionsNetworkDataSource
import com.zobaer53.zedmovies.data.network.source.MovieDetailsNetworkDataSource
import com.zobaer53.zedmovies.data.network.source.MovieNetworkDataSource
import com.zobaer53.zedmovies.data.network.source.TvShowDetailsNetworkDataSource
import com.zobaer53.zedmovies.data.network.source.TvShowNetworkDataSource
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
import com.zobaer53.zedmovies.domain.usecase.AddMovieToWishlistUseCase
import com.zobaer53.zedmovies.domain.usecase.AddTvShowToWishlistUseCase
import com.zobaer53.zedmovies.domain.usecase.DiscoverMoviesUseCase
import com.zobaer53.zedmovies.domain.usecase.DiscoverTvShowsUseCase
import com.zobaer53.zedmovies.domain.usecase.GetFilterOptionsUseCase
import com.zobaer53.zedmovies.domain.usecase.GetMovieDetailsUseCase
import com.zobaer53.zedmovies.domain.usecase.GetMoviesPagingUseCase
import com.zobaer53.zedmovies.domain.usecase.GetMoviesUseCase
import com.zobaer53.zedmovies.domain.usecase.GetTvShowDetailsUseCase
import com.zobaer53.zedmovies.domain.usecase.GetTvShowsPagingUseCase
import com.zobaer53.zedmovies.domain.usecase.GetTvShowsUseCase
import com.zobaer53.zedmovies.domain.usecase.GetWishlistMoviesUseCase
import com.zobaer53.zedmovies.domain.usecase.GetWishlistTvShowsUseCase
import com.zobaer53.zedmovies.domain.usecase.RemoveMovieFromWishlistUseCase
import com.zobaer53.zedmovies.domain.usecase.RemoveTvShowFromWishlistUseCase
import com.zobaer53.zedmovies.domain.usecase.SearchMoviesUseCase
import com.zobaer53.zedmovies.domain.usecase.SearchTvShowsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

// The platform-independent part of the DI graph (mirrors the former Hilt modules 1:1). It moves to
// shared/commonMain in Phase 3. The database, DataStore, API key and platform providers come from
// androidModule (AndroidModule.kt); the ViewModels from viewModelModule (ViewModelModule.kt).

val networkModule = module {
    single { zedMoviesApi(get<zedMoviesApiKeyProvider>()) }
    single { get<zedMoviesApi>().movieService }
    single { get<zedMoviesApi>().tvShowService }
    single { get<zedMoviesApi>().filterOptionsService }
}

val databaseModule = module {
    factory { get<zedMoviesDatabase>().movieDao }
    factory { get<zedMoviesDatabase>().tvShowDao }
    factory { get<zedMoviesDatabase>().movieRemoteKeyDao }
    factory { get<zedMoviesDatabase>().tvShowRemoteKeyDao }
    factory { get<zedMoviesDatabase>().movieDetailsDao }
    factory { get<zedMoviesDatabase>().tvShowDetailsDao }
    factory { get<zedMoviesDatabase>().wishlistDao }
    factoryOf(::zedMoviesDatabaseTransactionProvider)
}

val dataSourceModule = module {
    factoryOf(::MovieDatabaseDataSource)
    factoryOf(::TvShowDatabaseDataSource)
    factoryOf(::MovieDetailsDatabaseDataSource)
    factoryOf(::TvShowDetailsDatabaseDataSource)
    factoryOf(::WishlistDatabaseDataSource)
    factoryOf(::PreferencesDataStoreDataSource)
    factoryOf(::MovieNetworkDataSource)
    factoryOf(::TvShowNetworkDataSource)
    factoryOf(::MovieDetailsNetworkDataSource)
    factoryOf(::TvShowDetailsNetworkDataSource)
    factoryOf(::FilterOptionsNetworkDataSource)
}

// Hilt scoped these to the ViewModel: a new repository per ViewModel, so `factory` keeps that.
val repositoryModule = module {
    factoryOf(::MovieRepositoryImpl) bind MovieRepository::class
    factoryOf(::TvShowRepositoryImpl) bind TvShowRepository::class
    factoryOf(::MovieDetailsRepositoryImpl) bind MovieDetailsRepository::class
    factoryOf(::TvShowDetailsRepositoryImpl) bind TvShowDetailsRepository::class
    factoryOf(::WishlistRepositoryImpl) bind WishlistRepository::class
    // One in-memory cache of the filter options per process, as with Hilt's @Singleton.
    singleOf(::FilterOptionsRepositoryImpl) bind FilterOptionsRepository::class
}

val useCaseModule = module {
    factoryOf(::AddMovieToWishlistUseCase)
    factoryOf(::AddTvShowToWishlistUseCase)
    factoryOf(::DiscoverMoviesUseCase)
    factoryOf(::DiscoverTvShowsUseCase)
    factoryOf(::GetFilterOptionsUseCase)
    factoryOf(::GetMovieDetailsUseCase)
    factoryOf(::GetMoviesPagingUseCase)
    factoryOf(::GetMoviesUseCase)
    factoryOf(::GetTvShowDetailsUseCase)
    factoryOf(::GetTvShowsPagingUseCase)
    factoryOf(::GetTvShowsUseCase)
    factoryOf(::GetWishlistMoviesUseCase)
    factoryOf(::GetWishlistTvShowsUseCase)
    factoryOf(::RemoveMovieFromWishlistUseCase)
    factoryOf(::RemoveTvShowFromWishlistUseCase)
    factoryOf(::SearchMoviesUseCase)
    factoryOf(::SearchTvShowsUseCase)
}

val dataModules = listOf(networkModule, databaseModule, dataSourceModule, repositoryModule, useCaseModule)
