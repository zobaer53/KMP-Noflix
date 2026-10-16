
package com.zobaer53.zedmovies.ui.list.util

import com.zobaer53.zedmovies.data.model.MediaType
import com.zobaer53.zedmovies.data.model.MediaType.Common.Discover
import com.zobaer53.zedmovies.data.model.MediaType.Common.NowPlaying
import com.zobaer53.zedmovies.data.model.MediaType.Common.Popular
import com.zobaer53.zedmovies.data.model.MediaType.Common.TopRated
import com.zobaer53.zedmovies.data.model.MediaType.Common.Trending
import com.zobaer53.zedmovies.data.model.MediaType.Common.Upcoming
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.discover
import com.zobaer53.zedmovies.resources.most_popular
import com.zobaer53.zedmovies.resources.now_playing
import com.zobaer53.zedmovies.resources.top_rated
import com.zobaer53.zedmovies.resources.trending
import com.zobaer53.zedmovies.resources.upcoming_movies


internal fun MediaType.Common.asTitleResourceId() = mediaTypesTitleResources.getValue(this)

private val mediaTypesTitleResources = mapOf(
    Upcoming to Res.string.upcoming_movies,
    TopRated to Res.string.top_rated,
    Popular to Res.string.most_popular,
    NowPlaying to Res.string.now_playing,
    Discover to Res.string.discover,
    Trending to Res.string.trending
)
