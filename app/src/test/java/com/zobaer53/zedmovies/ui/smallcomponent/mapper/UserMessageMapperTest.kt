package com.zobaer53.zedmovies.ui.smallcomponent.mapper

import com.zobaer53.zedmovies.R
import com.zobaer53.zedmovies.ui.model.UserMessage
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.io.EOFException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Test

class UserMessageMapperTest {

    @Test
    fun `IOException and its subclasses map to NoInternet`() {
        listOf(IOException(), UnknownHostException("api.themoviedb.org"), SocketTimeoutException(), EOFException())
            .forEach { assertEquals(it.toString(), UserMessage.NoInternet, it.asUserMessage()) }
    }

    @Test
    fun `any other error maps to Unknown`() {
        listOf(IllegalStateException(), SerializationException("bad json"), RuntimeException(IOException()))
            .forEach { assertEquals(it.toString(), UserMessage.Unknown, it.asUserMessage()) }
    }

    @Test
    fun `every message keeps its string`() {
        assertEquals(R.string.no_internet_connection, UserMessage.NoInternet.messageResourceId)
        assertEquals(R.string.unknown_error, UserMessage.Unknown.messageResourceId)
        assertEquals(R.string.add_movie_wishlist, UserMessage.MovieAddedToWishlist.messageResourceId)
        assertEquals(R.string.remove_movie_wishlist, UserMessage.MovieRemovedFromWishlist.messageResourceId)
        assertEquals(R.string.add_tv_show_wishlist, UserMessage.TvShowAddedToWishlist.messageResourceId)
        assertEquals(R.string.remove_tv_show_wishlist, UserMessage.TvShowRemovedFromWishlist.messageResourceId)
    }
}
