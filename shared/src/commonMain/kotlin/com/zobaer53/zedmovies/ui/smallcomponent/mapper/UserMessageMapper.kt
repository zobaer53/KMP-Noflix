package com.zobaer53.zedmovies.ui.smallcomponent.mapper

import androidx.compose.runtime.Composable
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.add_movie_wishlist
import com.zobaer53.zedmovies.resources.add_tv_show_wishlist
import com.zobaer53.zedmovies.resources.no_internet_connection
import com.zobaer53.zedmovies.resources.remove_movie_wishlist
import com.zobaer53.zedmovies.resources.remove_tv_show_wishlist
import com.zobaer53.zedmovies.resources.unknown_error
import com.zobaer53.zedmovies.ui.model.UserMessage
import kotlinx.io.IOException
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun UserMessage.asString() = stringResource(messageResourceId)

internal val UserMessage.messageResourceId: StringResource
    get() = when (this) {
        UserMessage.NoInternet -> Res.string.no_internet_connection
        UserMessage.Unknown -> Res.string.unknown_error
        UserMessage.MovieAddedToWishlist -> Res.string.add_movie_wishlist
        UserMessage.MovieRemovedFromWishlist -> Res.string.remove_movie_wishlist
        UserMessage.TvShowAddedToWishlist -> Res.string.add_tv_show_wishlist
        UserMessage.TvShowRemovedFromWishlist -> Res.string.remove_tv_show_wishlist
    }

fun Throwable.asUserMessage(): UserMessage = when (this) {
    is IOException -> UserMessage.NoInternet
    else -> UserMessage.Unknown
}
