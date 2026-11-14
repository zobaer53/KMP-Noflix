package com.zobaer53.zedmovies.ios

import com.zobaer53.zedmovies.ui.playback.MediaLauncher
import com.zobaer53.zedmovies.ui.playback.PlayRequest
import com.zobaer53.zedmovies.ui.playback.STREAM_DOCUMENT_START_SCRIPT
import com.zobaer53.zedmovies.ui.playback.STREAM_PAGE_SCRIPT
import com.zobaer53.zedmovies.ui.playback.buildStreamUrl
import com.zobaer53.zedmovies.ui.playback.youtubeWatchUrl
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenURLOptionUniversalLinksOnly

/**
 * iOS MediaLauncher (doc/kmp-ios-plan.md, D7 and D10): the full-screen player is the Swift [host]'s job, trailers
 * open in the YouTube app, else Safari. Both methods are called from the Compose UI on the main thread.
 */
internal class IosMediaLauncher(private val host: IosPlaybackHost) : MediaLauncher {
    override fun play(request: PlayRequest) {
        host.presentPlayer(
            IosPlayerRequest(
                url = buildStreamUrl(request.tmdbId, request.type),
                title = "${request.name} ${request.year}".trim(),
                documentStartScript = STREAM_DOCUMENT_START_SCRIPT,
                pageScript = STREAM_PAGE_SCRIPT,
            ),
        )
    }

    /**
     * universalLinksOnly: the URL opens only if an app (the YouTube app) claims it. Otherwise the completion handler
     * gets false and the same URL opens in Safari. Needs no LSApplicationQueriesSchemes and no canOpenURL.
     */
    override fun playTrailer(youtubeKey: String) {
        val url = NSURL.URLWithString(youtubeWatchUrl(youtubeKey)) ?: return
        val application = UIApplication.sharedApplication
        application.openURL(url, options = mapOf(UIApplicationOpenURLOptionUniversalLinksOnly to true)) { opened ->
            if (!opened) application.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
        }
    }
}
