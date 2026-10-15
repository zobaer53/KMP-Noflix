package com.zobaer53.zedmovies.ui.webview

const val STREAM_BASE_URL = "https://framextv.xyz"

// The streaming site is a client-rendered SPA keyed by TMDB id, so the watch URL is built directly.
fun buildStreamUrl(tmdbId: Int, type: String): String =
    if (type.trim().lowercase() == "tv") {
        "$STREAM_BASE_URL/tv/$tmdbId"
    } else {
        "$STREAM_BASE_URL/watch/$tmdbId"
    }
