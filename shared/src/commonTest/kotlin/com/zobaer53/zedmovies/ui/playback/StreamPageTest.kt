package com.zobaer53.zedmovies.ui.playback

import io.ktor.http.parseUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreamPageTest {
    private val host = requireNotNull(parseUrl(STREAM_BASE_URL)).host

    @Test
    fun `buildStreamUrl builds the movie watch url`() {
        assertEquals("$STREAM_BASE_URL/watch/550", buildStreamUrl(550, "movie"))
    }

    @Test
    fun `buildStreamUrl builds the tv url for tv in any case or padding`() {
        assertEquals("$STREAM_BASE_URL/tv/1399", buildStreamUrl(1399, "tv"))
        assertEquals("$STREAM_BASE_URL/tv/1399", buildStreamUrl(1399, " TV "))
    }

    @Test
    fun `buildStreamUrl treats any other type as a movie`() {
        assertEquals("$STREAM_BASE_URL/watch/7", buildStreamUrl(7, ""))
    }

    @Test
    fun `isStreamSitePage accepts the site and its subdomains`() {
        assertTrue(isStreamSitePage(STREAM_BASE_URL))
        assertTrue(isStreamSitePage("$STREAM_BASE_URL/watch/1"))
        assertTrue(isStreamSitePage("http://$host/tv/1"))
        assertTrue(isStreamSitePage("https://www.$host/watch/1"))
        assertTrue(isStreamSitePage("https://${host.uppercase()}/watch/1"))
    }

    @Test
    fun `isStreamSitePage rejects look-alike hosts and other pages`() {
        assertFalse(isStreamSitePage("https://evil$host/watch/1"))
        assertFalse(isStreamSitePage("https://$host.evil.com/watch/1"))
        assertFalse(isStreamSitePage("https://example.com/?next=$STREAM_BASE_URL"))
        assertFalse(isStreamSitePage("https://example.com/$host"))
    }

    @Test
    fun `isStreamSitePage rejects null empty and non web urls`() {
        assertFalse(isStreamSitePage(null))
        assertFalse(isStreamSitePage(""))
        assertFalse(isStreamSitePage("about:blank"))
        assertFalse(isStreamSitePage("itms-apps://$host/watch/1"))
        assertFalse(isStreamSitePage("not a url"))
    }

    @Test
    fun `isStreamWatchPage is true only for watch paths on the site`() {
        assertTrue(isStreamWatchPage("$STREAM_BASE_URL/watch/550"))
        assertTrue(isStreamWatchPage("$STREAM_BASE_URL/watch/550?x=1#t"))
        assertFalse(isStreamWatchPage("$STREAM_BASE_URL/tv/1399"))
        assertFalse(isStreamWatchPage("$STREAM_BASE_URL/watching"))
        assertFalse(isStreamWatchPage("$STREAM_BASE_URL/"))
        assertFalse(isStreamWatchPage("https://example.com/watch/550"))
        assertFalse(isStreamWatchPage(null))
    }

    @Test
    fun `isStreamNavigationAllowed main frame allows only the site and about blank`() {
        assertTrue(isStreamNavigationAllowed("$STREAM_BASE_URL/watch/550", isMainFrame = true))
        assertTrue(isStreamNavigationAllowed("https://www.$host/tv/1", isMainFrame = true))
        assertTrue(isStreamNavigationAllowed("about:blank", isMainFrame = true))
        assertFalse(isStreamNavigationAllowed("https://example.com/ad", isMainFrame = true))
        assertFalse(isStreamNavigationAllowed("https://evil$host/", isMainFrame = true))
        assertFalse(isStreamNavigationAllowed("about:srcdoc", isMainFrame = true))
        assertFalse(isStreamNavigationAllowed("data:text/html,hi", isMainFrame = true))
        assertFalse(isStreamNavigationAllowed("blob:https://$host/1", isMainFrame = true))
        assertFalse(isStreamNavigationAllowed("itms-apps://apps.apple.com/app/id1", isMainFrame = true))
        assertFalse(isStreamNavigationAllowed(null, isMainFrame = true))
    }

    @Test
    fun `isStreamNavigationAllowed sub frames allow web about data and blob`() {
        assertTrue(isStreamNavigationAllowed("https://player.example.com/embed/1", isMainFrame = false))
        assertTrue(isStreamNavigationAllowed("http://player.example.com/embed/1", isMainFrame = false))
        assertTrue(isStreamNavigationAllowed("about:blank", isMainFrame = false))
        assertTrue(isStreamNavigationAllowed("about:srcdoc", isMainFrame = false))
        assertTrue(isStreamNavigationAllowed("data:text/html,hi", isMainFrame = false))
        assertTrue(isStreamNavigationAllowed("blob:https://player.example.com/1", isMainFrame = false))
    }

    @Test
    fun `isStreamNavigationAllowed cancels app and unknown schemes in every frame`() {
        for (mainFrame in listOf(true, false)) {
            assertFalse(isStreamNavigationAllowed("itms-apps://apps.apple.com/app/id1", mainFrame))
            assertFalse(isStreamNavigationAllowed("intent://scan/#Intent;scheme=zxing;end", mainFrame))
            assertFalse(isStreamNavigationAllowed("market://details?id=a.b", mainFrame))
            assertFalse(isStreamNavigationAllowed("javascript:alert(1)", mainFrame))
            assertFalse(isStreamNavigationAllowed("tel:123", mainFrame))
            assertFalse(isStreamNavigationAllowed("someapp://open", mainFrame))
            assertFalse(isStreamNavigationAllowed("", mainFrame))
        }
    }

    @Test
    fun `youtubeWatchUrl encodes the key`() {
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", youtubeWatchUrl("dQw4w9WgXcQ"))
        assertEquals("https://www.youtube.com/watch?v=a%26b%3Dc", youtubeWatchUrl("a&b=c"))
    }

    // Android quoted the CSS with org.json, the shared script uses JsonPrimitive plus the '/' escape. They agree for
    // printable ASCII plus newlines (:app's StreamPageScriptParityTest compares them on the real org.json).
    @Test
    fun `the css is printable ascii plus newlines`() {
        assertTrue(STREAM_PAGE_CSS.all { it == '\n' || it in ' '..'~' })
    }

    @Test
    fun `the page script embeds the css as one json string literal`() {
        val quoted = "\"" + STREAM_PAGE_CSS.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("/", "\\/") + "\""
        assertTrue(STREAM_PAGE_SCRIPT.contains("s.textContent = $quoted;"))
    }
}
