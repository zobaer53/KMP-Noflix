package com.zobaer53.zedmovies.desktop

import com.zobaer53.zedmovies.ui.playback.MediaLauncher
import com.zobaer53.zedmovies.ui.playback.PlayRequest
import com.zobaer53.zedmovies.ui.playback.buildStreamUrl
import com.zobaer53.zedmovies.ui.playback.youtubeWatchUrl
import java.awt.Desktop
import java.net.URI

/**
 * Desktop v1 playback: open the stream in the system browser. Ads are blocked by whatever blocker
 * the user has there — the app cannot block them inside another site's player. The UI says so.
 */
object DesktopMediaLauncher : MediaLauncher {
    override fun play(request: PlayRequest) = browse(buildStreamUrl(request.tmdbId, request.type))

    override fun playTrailer(youtubeKey: String) = browse(youtubeWatchUrl(youtubeKey))

    private fun browse(url: String) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}
