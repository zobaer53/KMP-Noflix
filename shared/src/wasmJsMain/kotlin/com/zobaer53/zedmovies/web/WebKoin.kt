package com.zobaer53.zedmovies.di

import com.zobaer53.zedmovies.data.platform.DeviceLocaleProvider
import com.zobaer53.zedmovies.data.platform.RegionProvider
import com.zobaer53.zedmovies.domain.repository.FilterOptionsRepository
import com.zobaer53.zedmovies.domain.repository.MovieDetailsRepository
import com.zobaer53.zedmovies.domain.repository.MovieRepository
import com.zobaer53.zedmovies.domain.repository.TvShowDetailsRepository
import com.zobaer53.zedmovies.domain.repository.TvShowRepository
import com.zobaer53.zedmovies.domain.repository.WishlistRepository
import com.zobaer53.zedmovies.web.WebFilterOptionsRepository
import com.zobaer53.zedmovies.web.WebMovieDetailsRepository
import com.zobaer53.zedmovies.web.WebMovieRepository
import com.zobaer53.zedmovies.web.WebSettings
import com.zobaer53.zedmovies.web.WebTvShowDetailsRepository
import com.zobaer53.zedmovies.web.WebTvShowRepository
import com.zobaer53.zedmovies.web.WebWishlistRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val cachedDataModules: List<Module> = listOf(
    module {
        factoryOf(::WebMovieRepository) bind MovieRepository::class
        factoryOf(::WebTvShowRepository) bind TvShowRepository::class
        factoryOf(::WebMovieDetailsRepository) bind MovieDetailsRepository::class
        factoryOf(::WebTvShowDetailsRepository) bind TvShowDetailsRepository::class
        factoryOf(::WebWishlistRepository) bind WishlistRepository::class
        singleOf(::WebFilterOptionsRepository) bind FilterOptionsRepository::class
    }
)

private object WebDeviceLocaleProvider : DeviceLocaleProvider {
    override fun language(): String = WebSettings.language()
}

private object WebRegionProvider : RegionProvider {
    override fun region(): String = WebSettings.region()
}

actual val platformModule: Module = module {
    single<DeviceLocaleProvider> { WebDeviceLocaleProvider }
    single<RegionProvider> { WebRegionProvider }
}
