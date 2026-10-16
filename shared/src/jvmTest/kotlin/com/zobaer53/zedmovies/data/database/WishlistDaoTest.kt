package com.zobaer53.zedmovies.data.database

import com.zobaer53.zedmovies.data.database.model.common.MediaType
import com.zobaer53.zedmovies.data.database.model.wishlist.WishlistEntity
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class WishlistDaoTest {
    private val database = inMemoryDatabase()
    private val dao = database.wishlistDao

    @AfterTest
    fun close() = database.close()

    @Test
    fun queriesByMediaType() = runTest {
        dao.insert(WishlistEntity(networkId = 550, mediaType = MediaType.Wishlist.Movie))
        dao.insert(WishlistEntity(networkId = 1399, mediaType = MediaType.Wishlist.TvShow))
        dao.insert(WishlistEntity(networkId = 680, mediaType = MediaType.Wishlist.Movie))

        assertEquals(listOf(550, 680), dao.getByMediaType(MediaType.Wishlist.Movie).first().map { it.networkId })
        assertEquals(listOf(1399), dao.getByMediaType(MediaType.Wishlist.TvShow).first().map { it.networkId })
    }

    @Test
    fun isWishlistedAndDelete() = runTest {
        dao.insert(WishlistEntity(networkId = 550, mediaType = MediaType.Wishlist.Movie))

        assertTrue(dao.isWishlisted(MediaType.Wishlist.Movie, 550))
        // Same TMDB id, other media type: a different item.
        assertFalse(dao.isWishlisted(MediaType.Wishlist.TvShow, 550))

        dao.deleteByMediaTypeAndNetworkId(MediaType.Wishlist.Movie, 550)

        assertFalse(dao.isWishlisted(MediaType.Wishlist.Movie, 550))
        assertTrue(dao.getByMediaType(MediaType.Wishlist.Movie).first().isEmpty())
    }
}
