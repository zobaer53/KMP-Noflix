package com.zobaer53.zedmovies.ui.webview

import com.zobaer53.zedmovies.ui.playback.STREAM_PAGE_CSS
import com.zobaer53.zedmovies.ui.playback.STREAM_PAGE_SCRIPT
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The page script moved from :app to :shared (ui/playback/StreamPage.kt), where it quotes the CSS with kotlinx
 * serialization instead of org.json (which also escapes every '/' as '\/'). Android must keep injecting the same
 * bytes: this test quotes the CSS with the real org.json (Robolectric) and checks that the shared script contains
 * exactly that text.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StreamPageScriptParityTest {
    @Test
    fun sharedScriptQuotesTheCssLikeOrgJson() {
        val expected = "s.textContent = " + JSONObject.quote(STREAM_PAGE_CSS) + ";"
        assertTrue("STREAM_PAGE_SCRIPT must contain: $expected", STREAM_PAGE_SCRIPT.contains(expected))
    }
}
