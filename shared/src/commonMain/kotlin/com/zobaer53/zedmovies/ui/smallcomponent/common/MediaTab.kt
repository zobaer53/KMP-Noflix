

package com.zobaer53.zedmovies.ui.smallcomponent.common

import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.movies
import com.zobaer53.zedmovies.resources.tv_shows
import org.jetbrains.compose.resources.StringResource


enum class MediaTab(val titleResourceId: StringResource) {
    Movies(titleResourceId = Res.string.movies),
    TvShows(titleResourceId = Res.string.tv_shows)
}
