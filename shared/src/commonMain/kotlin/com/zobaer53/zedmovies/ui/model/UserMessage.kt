package com.zobaer53.zedmovies.ui.model

/** A message shown to the user. [asString][com.zobaer53.zedmovies.ui.smallcomponent.mapper.asString] gives its text. */
sealed interface UserMessage {
    data object NoInternet : UserMessage
    data object Unknown : UserMessage
    data object MovieAddedToWishlist : UserMessage
    data object MovieRemovedFromWishlist : UserMessage
    data object TvShowAddedToWishlist : UserMessage
    data object TvShowRemovedFromWishlist : UserMessage
}
