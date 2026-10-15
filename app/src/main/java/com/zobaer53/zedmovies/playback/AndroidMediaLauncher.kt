package com.zobaer53.zedmovies.playback

import android.content.Context
import android.content.Intent
import com.zobaer53.zedmovies.ui.playback.MediaLauncher
import com.zobaer53.zedmovies.ui.playback.PlayRequest
import com.zobaer53.zedmovies.ui.trailer.TrailerActivity
import com.zobaer53.zedmovies.ui.webview.WebViewActivity

/** Starts the WebView player and the trailer player. [context] must be an Activity (no NEW_TASK flag). */
class AndroidMediaLauncher(private val context: Context) : MediaLauncher {
    override fun play(request: PlayRequest) = context.startActivity(
        Intent(context, WebViewActivity::class.java).apply {
            putExtra("movieName", request.name)
            putExtra("movieYear", request.year)
            putExtra("type", request.type)
            putExtra("tmdbId", request.tmdbId)
        }
    )

    override fun playTrailer(youtubeKey: String) =
        context.startActivity(TrailerActivity.intent(context, youtubeKey))
}
