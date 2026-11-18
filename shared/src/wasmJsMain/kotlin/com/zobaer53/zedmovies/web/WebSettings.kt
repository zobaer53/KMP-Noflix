package com.zobaer53.zedmovies.web

import kotlinx.browser.localStorage
import kotlinx.browser.window

/** The web counterpart of PreferencesDataStoreDataSource + WishlistDatabaseDataSource. */
internal object WebSettings {
    private const val ContentLanguageKey = "noflix.content_language"
    private const val WishlistMoviesKey = "noflix.wishlist.movies"
    private const val WishlistTvShowsKey = "noflix.wishlist.tv"

    fun contentLanguage(): String = localStorage.getItem(ContentLanguageKey)?.takeIf { it.isNotBlank() } ?: "en-US"

    fun setContentLanguage(language: String) = localStorage.setItem(ContentLanguageKey, language)

    fun wishlist(isTvShow: Boolean): List<Int> = localStorage.getItem(key(isTvShow))
        ?.split(',')
        ?.mapNotNull { it.trim().toIntOrNull() }
        .orEmpty()

    fun setWishlist(isTvShow: Boolean, ids: List<Int>) =
        localStorage.setItem(key(isTvShow), ids.joinToString(","))

    fun region(): String = window.navigator.language.substringAfter('-', "").uppercase()

    fun language(): String = window.navigator.language.substringBefore('-').lowercase()

    private fun key(isTvShow: Boolean) = if (isTvShow) WishlistTvShowsKey else WishlistMoviesKey
}
