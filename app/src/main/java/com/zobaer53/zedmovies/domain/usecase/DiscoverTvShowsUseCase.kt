package com.zobaer53.zedmovies.domain.usecase

import com.zobaer53.zedmovies.domain.model.SearchFiltersModel
import com.zobaer53.zedmovies.domain.repository.TvShowRepository

class DiscoverTvShowsUseCase(private val repository: TvShowRepository) {
    operator fun invoke(filters: SearchFiltersModel) = repository.discover(filters)
}
