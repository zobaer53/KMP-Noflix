package com.zobaer53.zedmovies.ios

import com.zobaer53.zedmovies.ui.playback.MediaLauncher
import com.zobaer53.zedmovies.ui.playback.PlayRequest

/** iOS MediaLauncher. I0 stub: both methods do nothing; I3 implements them (doc/kmp-ios-plan.md). */
internal class IosMediaLauncher(private val host: IosPlaybackHost) : MediaLauncher {
    override fun play(request: PlayRequest) = Unit

    override fun playTrailer(youtubeKey: String) = Unit
}
