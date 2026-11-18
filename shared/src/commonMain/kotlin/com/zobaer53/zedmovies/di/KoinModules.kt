package com.zobaer53.zedmovies.di

import com.zobaer53.zedmovies.data.network.api.zedMoviesApi
import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import com.zobaer53.zedmovies.data.network.source.FilterOptionsNetworkDataSource
import com.zobaer53.zedmovies.data.network.source.MovieDetailsNetworkDataSource
import com.zobaer53.zedmovies.data.network.source.MovieNetworkDataSource
import com.zobaer53.zedmovies.data.network.source.TvShowDetailsNetworkDataSource
import com.zobaer53.zedmovies.data.network.source.TvShowNetworkDataSource
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
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

// The platform-independent part of the DI graph. The Room/DataStore repositories live in
// cachedMain; wasmJsMain provides network-only repositories instead. The platform module supplies
// the database, DataStore and locale/region providers (or browser storage on web).

val networkModule = module {
    single { zedMoviesApi(get<zedMoviesApiKeyProvider>()) }
    single { get<zedMoviesApi>().movieService }
    single { get<zedMoviesApi>().tvShowService }
    single { get<zedMoviesApi>().filterOptionsService }
}

val networkDataSourceModule = module {
    factoryOf(::MovieNetworkDataSource)
    factoryOf(::TvShowNetworkDataSource)
    factoryOf(::MovieDetailsNetworkDataSource)
    factoryOf(::TvShowDetailsNetworkDataSource)
    factoryOf(::FilterOptionsNetworkDataSource)
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

/** Room/DataStore-backed data sources and repositories on cached platforms; network-only on web. */
expect val cachedDataModules: List<Module>

val dataModules: List<Module>
    get() = listOf(networkModule, networkDataSourceModule, useCaseModule) + cachedDataModules

/** The database, DataStore and locale/region providers of each platform. */
expect val platformModule: Module

/** Everything :shared contributes to the graph. The app still has to provide `zedMoviesApiKeyProvider`. */
val sharedModules: List<Module> get() = dataModules + platformModule
