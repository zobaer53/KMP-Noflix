package com.zobaer53.zedmovies.ui.webview

import android.net.Uri
import com.zobaer53.zedmovies.ui.playback.STREAM_BASE_URL

// The stream constants and page scripts live in :shared (ui/playback/StreamPage.kt), shared with iOS.
fun isStreamSiteUrl(url: String?): Boolean =
    url != null && Uri.parse(url).host?.endsWith(Uri.parse(STREAM_BASE_URL).host!!) == true

fun isStreamWatchUrl(url: String?): Boolean =
    isStreamSiteUrl(url) && Uri.parse(url).path?.startsWith("/watch/") == true
