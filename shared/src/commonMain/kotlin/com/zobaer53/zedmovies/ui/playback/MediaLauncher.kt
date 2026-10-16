package com.zobaer53.zedmovies.ui.playback

import androidx.compose.runtime.staticCompositionLocalOf

// Common so the shared Play button can use it; the Android implementation (playback/AndroidMediaLauncher.kt) stays in the app.

/** What the Play button needs to start playback. [type] "tv" plays a TV show, anything else a movie. */
data class PlayRequest(val tmdbId: Int, val type: String, val name: String, val year: String)

/** Starts playback outside the Compose UI. */
interface MediaLauncher {
    fun play(request: PlayRequest)
    fun playTrailer(youtubeKey: String)
}

val LocalMediaLauncher = staticCompositionLocalOf<MediaLauncher> { error("No MediaLauncher provided") }
