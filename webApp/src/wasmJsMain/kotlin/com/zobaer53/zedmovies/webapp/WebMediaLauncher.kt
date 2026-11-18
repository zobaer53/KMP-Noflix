package com.zobaer53.zedmovies.webapp

import com.zobaer53.zedmovies.ui.playback.MediaLauncher
import com.zobaer53.zedmovies.ui.playback.PlayRequest
import com.zobaer53.zedmovies.ui.playback.buildStreamUrl
import com.zobaer53.zedmovies.ui.playback.youtubeWatchUrl
import kotlinx.browser.window

/**
 * Web v1 playback: open the stream page in a new tab. The app cannot block ads inside another
 * site's tab, so the shared UI must say so. Trailers open as a normal YouTube tab.
 */
object WebMediaLauncher : MediaLauncher {
    override fun play(request: PlayRequest) = open(buildStreamUrl(request.tmdbId, request.type))

    override fun playTrailer(youtubeKey: String) = open(youtubeWatchUrl(youtubeKey))

    private fun open(url: String) {
        window.open(url, "_blank", "noopener")
    }
}
