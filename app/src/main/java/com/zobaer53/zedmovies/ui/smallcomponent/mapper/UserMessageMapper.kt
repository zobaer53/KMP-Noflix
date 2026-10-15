package com.zobaer53.zedmovies.ui.smallcomponent.mapper

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zobaer53.zedmovies.R
import com.zobaer53.zedmovies.ui.model.UserMessage
import kotlinx.io.IOException

@Composable
fun UserMessage.asString() = stringResource(id = messageResourceId)

@get:StringRes
internal val UserMessage.messageResourceId: Int
    get() = when (this) {
        UserMessage.NoInternet -> R.string.no_internet_connection
        UserMessage.Unknown -> R.string.unknown_error
        UserMessage.MovieAddedToWishlist -> R.string.add_movie_wishlist
        UserMessage.MovieRemovedFromWishlist -> R.string.remove_movie_wishlist
        UserMessage.TvShowAddedToWishlist -> R.string.add_tv_show_wishlist
        UserMessage.TvShowRemovedFromWishlist -> R.string.remove_tv_show_wishlist
    }

fun Throwable.asUserMessage(): UserMessage = when (this) {
    is IOException -> UserMessage.NoInternet
    else -> UserMessage.Unknown
}
