package com.zobaer53.zedmovies.navigation

import com.zobaer53.zedmovies.ui.home.navigation.HomeDestination
import com.zobaer53.zedmovies.ui.search.navigation.SearchDestination
import com.zobaer53.zedmovies.ui.wishlist.navigation.WishlistDestination
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.home
import com.zobaer53.zedmovies.resources.ic_home
import com.zobaer53.zedmovies.resources.ic_search
import com.zobaer53.zedmovies.resources.ic_wishlist
import com.zobaer53.zedmovies.resources.search
import com.zobaer53.zedmovies.resources.wishlist
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class TopLevelDestination(
    override val route: String,
    override val destination: String,
    val iconResourceId: DrawableResource,
    val textResourceId: StringResource
) : zedMoviesNavigationDestination {
    Home(
        route = HomeDestination.route,
        destination = HomeDestination.destination,
        iconResourceId = Res.drawable.ic_home,
        textResourceId = Res.string.home
    ),
    Search(
        route = SearchDestination.route,
        destination = SearchDestination.destination,
        iconResourceId = Res.drawable.ic_search,
        textResourceId = Res.string.search
    ),
    Wishlist(
        route = WishlistDestination.route,
        destination = WishlistDestination.destination,
        iconResourceId = Res.drawable.ic_wishlist,
        textResourceId = Res.string.wishlist
    )

}
