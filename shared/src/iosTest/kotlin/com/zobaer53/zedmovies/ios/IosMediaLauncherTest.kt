package com.zobaer53.zedmovies.ios

import com.zobaer53.zedmovies.ui.playback.PlayRequest
import com.zobaer53.zedmovies.ui.playback.STREAM_BASE_URL
import com.zobaer53.zedmovies.ui.playback.STREAM_DOCUMENT_START_SCRIPT
import com.zobaer53.zedmovies.ui.playback.STREAM_PAGE_SCRIPT
import kotlin.test.Test
import kotlin.test.assertEquals

class IosMediaLauncherTest {
    private class FakeHost : IosPlaybackHost {
        val requests = mutableListOf<IosPlayerRequest>()

        override fun presentPlayer(request: IosPlayerRequest) {
            requests += request
        }
    }

    @Test
    fun `play presents a movie player with the shared scripts`() {
        val host = FakeHost()
        IosMediaLauncher(host).play(PlayRequest(tmdbId = 550, type = "movie", name = "Fight Club", year = "1999"))

        val request = host.requests.single()
        assertEquals("$STREAM_BASE_URL/watch/550", request.url)
        assertEquals("Fight Club 1999", request.title)
        assertEquals(STREAM_DOCUMENT_START_SCRIPT, request.documentStartScript)
        assertEquals(STREAM_PAGE_SCRIPT, request.pageScript)
    }

    @Test
    fun `play presents a tv player`() {
        val host = FakeHost()
        IosMediaLauncher(host).play(PlayRequest(tmdbId = 1399, type = "tv", name = "Game of Thrones", year = ""))

        val request = host.requests.single()
        assertEquals("$STREAM_BASE_URL/tv/1399", request.url)
        assertEquals("Game of Thrones", request.title)
    }
}
