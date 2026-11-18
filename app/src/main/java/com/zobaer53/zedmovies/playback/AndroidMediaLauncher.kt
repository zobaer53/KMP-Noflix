package com.zobaer53.zedmovies.playback

import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.core.net.toUri
import com.zobaer53.zedmovies.ui.playback.MediaLauncher
import com.zobaer53.zedmovies.ui.playback.PlayRequest
import com.zobaer53.zedmovies.ui.trailer.TrailerActivity
import com.zobaer53.zedmovies.ui.webview.WebViewActivity

/** Starts the WebView player and the trailer player. [context] must be an Activity (no NEW_TASK flag). */
class AndroidMediaLauncher(private val context: Context) : MediaLauncher {
    private val isTv: Boolean
        get() = context.getSystemService(UiModeManager::class.java)?.currentModeType ==
                Configuration.UI_MODE_TYPE_TELEVISION

    override fun play(request: PlayRequest) = context.startActivity(
        Intent(context, WebViewActivity::class.java).apply {
            putExtra("movieName", request.name)
            putExtra("movieYear", request.year)
            putExtra("type", request.type)
            putExtra("tmdbId", request.tmdbId)
        }
    )

    override fun playTrailer(youtubeKey: String) {
        // The YouTube TV app handles the vnd.youtube scheme and is far more pleasant with a remote
        // than the embedded player. Fall back to the in-app player when it isn't installed.
        if (isTv) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, "vnd.youtube:$youtubeKey".toUri()))
                return
            } catch (_: ActivityNotFoundException) {
                // fall through to TrailerActivity
            }
        }
        context.startActivity(TrailerActivity.intent(context, youtubeKey))
    }
}
