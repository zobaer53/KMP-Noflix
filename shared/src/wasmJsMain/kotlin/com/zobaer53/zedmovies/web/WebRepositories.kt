package com.zobaer53.zedmovies.web

import androidx.paging.Pager
import androidx.paging.PagingData
import com.zobaer53.zedmovies.data.common.result.ZedMoviesResult
import com.zobaer53.zedmovies.data.common.result.map
import com.zobaer53.zedmovies.data.mapper.asCertificationsModel
import com.zobaer53.zedmovies.data.mapper.asDiscoverMovieQuery
import com.zobaer53.zedmovies.data.mapper.asDiscoverTvQuery
import com.zobaer53.zedmovies.data.mapper.asWatchProvidersModel
import com.zobaer53.zedmovies.data.mapper.filtersSearchResults
import com.zobaer53.zedmovies.data.mapper.matches
import com.zobaer53.zedmovies.data.mapper.searchYear
import com.zobaer53.zedmovies.data.network.model.common.NetworkMediaType
import com.zobaer53.zedmovies.data.network.model.movie.NetworkMovie
import com.zobaer53.zedmovies.data.network.model.response.MovieResponseDto
import com.zobaer53.zedmovies.data.network.model.response.TvShowResponseDto
import com.zobaer53.zedmovies.data.network.model.tvshow.NetworkTvShow
import com.zobaer53.zedmovies.data.network.source.FilterOptionsNetworkDataSource
import com.zobaer53.zedmovies.data.network.source.MovieDetailsNetworkDataSource
import com.zobaer53.zedmovies.data.network.source.MovieNetworkDataSource
import com.zobaer53.zedmovies.data.network.source.TvShowDetailsNetworkDataSource
import com.zobaer53.zedmovies.data.network.source.TvShowNetworkDataSource
import com.zobaer53.zedmovies.data.network.util.Constants.Discover
import com.zobaer53.zedmovies.data.paging.FILTERED_REQUESTS_PER_LOAD
import com.zobaer53.zedmovies.data.paging.ResultsPage
import com.zobaer53.zedmovies.data.paging.TmdbPagingSource
import com.zobaer53.zedmovies.data.platform.RegionProvider
import com.zobaer53.zedmovies.data.util.defaultPagingConfig
import com.zobaer53.zedmovies.domain.model.FilterOptionsModel
import com.zobaer53.zedmovies.domain.model.MediaTypeModel
import com.zobaer53.zedmovies.domain.model.MovieDetailsModel
import com.zobaer53.zedmovies.domain.model.MovieModel
import com.zobaer53.zedmovies.domain.model.SearchFiltersModel
import com.zobaer53.zedmovies.domain.model.TvShowDetailsModel
import com.zobaer53.zedmovies.domain.model.TvShowModel
import com.zobaer53.zedmovies.domain.model.WishlistModel
import com.zobaer53.zedmovies.domain.repository.FilterOptionsRepository
import com.zobaer53.zedmovies.domain.repository.MovieDetailsRepository
import com.zobaer53.zedmovies.domain.repository.MovieRepository
import com.zobaer53.zedmovies.domain.repository.TvShowDetailsRepository
import com.zobaer53.zedmovies.domain.repository.TvShowRepository
import com.zobaer53.zedmovies.domain.repository.WishlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Network-only movie repository for the web: no Room cache, no RemoteMediator. */
internal class WebMovieRepository(
    private val network: MovieNetworkDataSource
) : MovieRepository {
    override fun getByMediaType(mediaTypeModel: MediaTypeModel.Movie): Flow<ZedMoviesResult<List<MovieModel>>> =
        flow {
            emit(ZedMoviesResult.loading())
            val mediaType = NetworkMediaType.Movie[mediaTypeModel.mediaType]
            emit(network.getByMediaType(mediaType, WebSettings.contentLanguage()).map { response ->
                response.results.map { it.asMovieModel() }
            })
        }

    override fun getPagingByMediaType(mediaTypeModel: MediaTypeModel.Movie): Flow<PagingData<MovieModel>> =
        Pager(config = defaultPagingConfig, pagingSourceFactory = {
            val mediaType = NetworkMediaType.Movie[mediaTypeModel.mediaType]
            TmdbPagingSource { page ->
                network.getByMediaType(mediaType, WebSettings.contentLanguage(), page).map { response ->
                    response.asResultsPage()
                }
            }
        }).flow

    override fun search(query: String, filters: SearchFiltersModel): Flow<PagingData<MovieModel>> =
        Pager(config = defaultPagingConfig, pagingSourceFactory = {
            val requestsPerLoad = if (filters.filtersSearchResults) FILTERED_REQUESTS_PER_LOAD else 1
            TmdbPagingSource(maxRequestsPerLoad = requestsPerLoad) { page ->
                network.search(query, WebSettings.contentLanguage(), page, filters.searchYear).map { response ->
                    response.asResultsPage { filters.matches(it) }
                }
            }
        }).flow

    override fun discover(filters: SearchFiltersModel): Flow<PagingData<MovieModel>> =
        Pager(config = defaultPagingConfig, pagingSourceFactory = {
            val query = filters.asDiscoverMovieQuery()
            TmdbPagingSource(maxPage = Discover.MAX_PAGE) { page ->
                network.discover(query, WebSettings.contentLanguage(), page).map(MovieResponseDto::asResultsPage)
            }
        }).flow
}

/** Network-only TV repository for the web. */
internal class WebTvShowRepository(
    private val network: TvShowNetworkDataSource
) : TvShowRepository {
    override fun getByMediaType(mediaTypeModel: MediaTypeModel.TvShow): Flow<ZedMoviesResult<List<TvShowModel>>> =
        flow {
            emit(ZedMoviesResult.loading())
            val mediaType = NetworkMediaType.TvShow[mediaTypeModel.mediaType]
            emit(network.getByMediaType(mediaType, WebSettings.contentLanguage()).map { response ->
                response.results.map { it.asTvShowModel() }
            })
        }

    override fun getPagingByMediaType(mediaTypeModel: MediaTypeModel.TvShow): Flow<PagingData<TvShowModel>> =
        Pager(config = defaultPagingConfig, pagingSourceFactory = {
            val mediaType = NetworkMediaType.TvShow[mediaTypeModel.mediaType]
            TmdbPagingSource { page ->
                network.getByMediaType(mediaType, WebSettings.contentLanguage(), page).map { response ->
                    response.asResultsPage()
                }
            }
        }).flow

    override fun search(query: String, filters: SearchFiltersModel): Flow<PagingData<TvShowModel>> =
        Pager(config = defaultPagingConfig, pagingSourceFactory = {
            val requestsPerLoad = if (filters.filtersSearchResults) FILTERED_REQUESTS_PER_LOAD else 1
            TmdbPagingSource(maxRequestsPerLoad = requestsPerLoad) { page ->
                network.search(query, WebSettings.contentLanguage(), page, filters.searchYear).map { response ->
                    response.asResultsPage { filters.matches(it) }
                }
            }
        }).flow

    override fun discover(filters: SearchFiltersModel): Flow<PagingData<TvShowModel>> =
        Pager(config = defaultPagingConfig, pagingSourceFactory = {
            val query = filters.asDiscoverTvQuery()
            TmdbPagingSource(maxPage = Discover.MAX_PAGE) { page ->
                network.discover(query, WebSettings.contentLanguage(), page).map(TvShowResponseDto::asResultsPage)
            }
        }).flow
}

internal class WebMovieDetailsRepository(
    private val network: MovieDetailsNetworkDataSource
) : MovieDetailsRepository {
    override fun getById(id: Int): Flow<ZedMoviesResult<MovieDetailsModel?>> = flow {
        emit(ZedMoviesResult.loading())
        emit(network.getById(id, WebSettings.contentLanguage()).map { details ->
            details?.asMovieDetailsModel(WebSettings.contentLanguage(), WebSettings.wishlist(false).contains(id))
        })
    }

    override fun getByIds(ids: List<Int>): Flow<ZedMoviesResult<List<MovieDetailsModel>>> = flow {
        emit(ZedMoviesResult.loading())
        val language = WebSettings.contentLanguage()
        val wishlist = WebSettings.wishlist(false)
        val loaded = ids.mapNotNull { id ->
            val result = network.getById(id, language)
            if (result is ZedMoviesResult.Success) result.value?.asMovieDetailsModel(language, wishlist.contains(id))
            else null
        }
        emit(ZedMoviesResult.success(loaded))
    }
}

internal class WebTvShowDetailsRepository(
    private val network: TvShowDetailsNetworkDataSource
) : TvShowDetailsRepository {
    override fun getById(id: Int): Flow<ZedMoviesResult<TvShowDetailsModel?>> = flow {
        emit(ZedMoviesResult.loading())
        emit(network.getById(id, WebSettings.contentLanguage()).map { details ->
            details?.asTvShowDetailsModel(WebSettings.contentLanguage(), WebSettings.wishlist(true).contains(id))
        })
    }

    override fun getByIds(ids: List<Int>): Flow<ZedMoviesResult<List<TvShowDetailsModel>>> = flow {
        emit(ZedMoviesResult.loading())
        val language = WebSettings.contentLanguage()
        val wishlist = WebSettings.wishlist(true)
        val loaded = ids.mapNotNull { id ->
            val result = network.getById(id, language)
            if (result is ZedMoviesResult.Success) result.value?.asTvShowDetailsModel(language, wishlist.contains(id))
            else null
        }
        emit(ZedMoviesResult.success(loaded))
    }
}

internal class WebWishlistRepository : WishlistRepository {
    override fun getMovies(): Flow<List<WishlistModel>> = flow {
        emit(WebSettings.wishlist(false).map { WishlistModel(it, MediaTypeModel.Wishlist.Movie) })
    }

    override fun getTvShows(): Flow<List<WishlistModel>> = flow {
        emit(WebSettings.wishlist(true).map { WishlistModel(it, MediaTypeModel.Wishlist.TvShow) })
    }

    override suspend fun addMovieToWishlist(id: Int) = WebSettings.setWishlist(false, WebSettings.wishlist(false) + id)

    override suspend fun addTvShowToWishlist(id: Int) = WebSettings.setWishlist(true, WebSettings.wishlist(true) + id)

    override suspend fun removeMovieFromWishlist(id: Int) =
        WebSettings.setWishlist(false, WebSettings.wishlist(false) - id)

    override suspend fun removeTvShowFromWishlist(id: Int) =
        WebSettings.setWishlist(true, WebSettings.wishlist(true) - id)
}

internal class WebFilterOptionsRepository(
    private val network: FilterOptionsNetworkDataSource,
    private val regionProvider: RegionProvider
) : FilterOptionsRepository {
    private var cached: FilterOptionsModel? = null

    override suspend fun getFilterOptions(): ZedMoviesResult<FilterOptionsModel> {
        cached?.let { return ZedMoviesResult.success(it) }
        val region = regionProvider.region().ifBlank { FALLBACK_REGION }
        val language = WebSettings.contentLanguage()
        val certifications = network.getMovieCertifications()
        if (certifications is ZedMoviesResult.Failure) return ZedMoviesResult.failure(certifications.error)
        val movieProviders = network.getMovieWatchProviders(region, language)
        if (movieProviders is ZedMoviesResult.Failure) return ZedMoviesResult.failure(movieProviders.error)
        val tvProviders = network.getTvWatchProviders(region, language)
        if (tvProviders is ZedMoviesResult.Failure) return ZedMoviesResult.failure(tvProviders.error)
        val movieResults = (movieProviders as ZedMoviesResult.Success).value.results
        val tvResults = (tvProviders as ZedMoviesResult.Success).value.results
        val options = FilterOptionsModel(
            certifications = (certifications as ZedMoviesResult.Success).value
                .asCertificationsModel(region, FALLBACK_REGION),
            watchProviders = (movieResults + tvResults).asWatchProvidersModel(region)
        )
        cached = options
        return ZedMoviesResult.success(options)
    }
}

private fun MovieResponseDto.asResultsPage(keep: (NetworkMovie) -> Boolean = { true }) = ResultsPage(
    results = results.filter(keep).map { it.asMovieModel() },
    totalPages = totalPages
)

private fun TvShowResponseDto.asResultsPage(keep: (NetworkTvShow) -> Boolean = { true }) = ResultsPage(
    results = results.filter(keep).map { it.asTvShowModel() },
    totalPages = totalPages
)

private const val FALLBACK_REGION = "US"
