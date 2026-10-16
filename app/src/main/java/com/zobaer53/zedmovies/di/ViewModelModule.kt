package com.zobaer53.zedmovies.di

import com.zobaer53.zedmovies.ui.details.DetailsViewModel
import com.zobaer53.zedmovies.ui.home.HomeViewModel
import com.zobaer53.zedmovies.ui.list.ListViewModel
import com.zobaer53.zedmovies.ui.search.SearchViewModel
import com.zobaer53.zedmovies.ui.wishlist.WishlistViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Stays in :app while the ViewModels do; moves to :shared with them in Phase 4.
// viewModelOf passes the SavedStateHandle from the NavBackStackEntry's CreationExtras, so the
// nav arguments (Details, List) and Search's saved filters keep working.
val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::ListViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::DetailsViewModel)
    viewModelOf(::WishlistViewModel)
}
