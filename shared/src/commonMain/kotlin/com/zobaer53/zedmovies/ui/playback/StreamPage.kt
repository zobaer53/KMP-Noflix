package com.zobaer53.zedmovies.ui.playback

import io.ktor.http.Url
import io.ktor.http.encodeURLParameter
import io.ktor.http.parseUrl
import kotlinx.serialization.json.JsonPrimitive

// The stream site's constants, page scripts and URL decisions, shared by Android (WebView) and iOS (WKWebView).
// The scripts break whenever the site's DOM changes, so there is one copy. :app keeps only the android.net.Uri
// helpers (isStreamSiteUrl, isStreamWatchUrl in ui/webview/StreamPageStyle.kt).

const val STREAM_BASE_URL = "https://framextv.xyz"

// The streaming site is a client-rendered SPA keyed by TMDB id, so the watch URL is built directly.
fun buildStreamUrl(tmdbId: Int, type: String): String =
    if (type.trim().lowercase() == "tv") {
        "$STREAM_BASE_URL/tv/$tmdbId"
    } else {
        "$STREAM_BASE_URL/watch/$tmdbId"
    }

// The site's welcome popup reads this flag once, when its scripts start, so it has to be
// set before they run (STREAM_PAGE_SCRIPT also sets it, but only after the page loads).
const val STREAM_DOCUMENT_START_SCRIPT =
    "try { localStorage.setItem('framextv_welcome_seen', 'true'); } catch (e) {}"

// Hides the site's logo header, bottom menu bar and footer, plus the "Cast" and
// "More Like This" sections (tagged nfx-hidden by the script below). On /watch/ pages
// the player iframe is stretched over the whole viewport so the video plays full screen.
// Public so :app's parity test can check the Android script. Keep it printable ASCII plus newlines, so that
// quoteForScript gives the same text as org.json's JSONObject.quote (StreamPageTest, StreamPageScriptParityTest).
const val STREAM_PAGE_CSS = """
header.sticky.top-0,
nav.fixed.bottom-0,
#root footer,
section.nfx-hidden { display: none !important; }

html.nfx-watch, html.nfx-watch body { overflow: hidden !important; background: #000 !important; }
html.nfx-watch iframe[data-testid="player-iframe"] {
  position: fixed !important;
  inset: 0 !important;
  /* 100% rather than vw/vh: vh resolves to 0 inside the Compose-hosted WebView. */
  width: 100% !important;
  height: 100% !important;
  z-index: 2147483647 !important;
}
"""

// A JSON string literal, escaped like Android's org.json (JSONObject.quote), which the script used before the move:
// kotlinx escapes quotes, backslashes and control characters the same way, and org.json also writes every '/' as '\/'.
// '\/' is the same character in JavaScript, but keeping the bytes identical means Android's injected text didn't change.
private fun quoteForScript(text: String): String = JsonPrimitive(text).toString().replace("/", "\\/")

// Idempotent: safe to run on every page callback. The site is a SPA, so route
// changes are tracked through history.pushState/replaceState and popstate.
val STREAM_PAGE_SCRIPT = """
(function () {
  try { localStorage.setItem('framextv_welcome_seen', 'true'); } catch (e) {}
  var d = document;
  if (!d.getElementById('nfx-style')) {
    var s = d.createElement('style');
    s.id = 'nfx-style';
    s.textContent = ${quoteForScript(STREAM_PAGE_CSS)};
    (d.head || d.documentElement).appendChild(s);
  }
  function sync() {
    d.documentElement.classList.toggle('nfx-watch', location.pathname.indexOf('/watch/') === 0);
  }
  // These sections have no stable class names, so they are matched by heading text.
  function hideSections() {
    d.querySelectorAll('section h2, section h3').forEach(function (h) {
      if (/^(Cast\b|More Like This)/i.test(h.textContent.trim())) {
        h.closest('section').classList.add('nfx-hidden');
      }
    });
  }
  sync();
  hideSections();
  if (!window.__nfxHooked) {
    new MutationObserver(hideSections).observe(d.documentElement, { childList: true, subtree: true });
    window.__nfxHooked = true;
    ['pushState', 'replaceState'].forEach(function (m) {
      var original = history[m];
      history[m] = function () {
        var result = original.apply(this, arguments);
        sync();
        return result;
      };
    });
    window.addEventListener('popstate', sync);
  }
})();
"""

private val streamHost: String = requireNotNull(parseUrl(STREAM_BASE_URL)).host.lowercase()

// The site's name without its top-level domain. The site moves between TLDs and redirects the old one with a 301
// (seen in 2026-11: the base URL answers with a permanent redirect to the same name on another TLD), so the same
// name on any single-label TLD counts as the site. Ad and pop-under hosts don't share the name.
private val streamSiteName: String = streamHost.substringBeforeLast('.')

private val schemeRegex = Regex("^([A-Za-z][A-Za-z0-9+.-]*):")

private fun schemeOf(url: String): String? = schemeRegex.find(url.trim())?.groupValues?.get(1)?.lowercase()

/** The parsed http(s) URL, or null for anything else (other schemes, relative or malformed strings). */
private fun parseWebUrl(url: String?): Url? {
    if (url == null) return null
    val scheme = schemeOf(url)
    if (scheme != "http" && scheme != "https") return null
    return parseUrl(url.trim())
}

/**
 * True if [url] is on the stream site: its host or a subdomain (dot-bounded), also on another single-label TLD
 * (the site's own redirects to a new domain must not be cancelled).
 */
fun isStreamSitePage(url: String?): Boolean {
    val host = parseWebUrl(url)?.host?.lowercase()?.trimEnd('.') ?: return false
    if (host == streamHost || host.endsWith(".$streamHost")) return true
    val withoutTld = host.substringBeforeLast('.', missingDelimiterValue = "")
    val tld = host.substringAfterLast('.')
    return tld.length >= 2 && tld.all { it in 'a'..'z' } &&
        (withoutTld == streamSiteName || withoutTld.endsWith(".$streamSiteName"))
}

/** True if [url] is a stream-site page whose path starts with /watch/ (the landscape player page). */
fun isStreamWatchPage(url: String?): Boolean =
    isStreamSitePage(url) && parseWebUrl(url)?.encodedPath?.startsWith("/watch/") == true

/**
 * The WKWebView navigation policy. Main frame (or a new window: pass isMainFrame = true): allow only http(s)
 * on the stream site, plus about:blank. Sub-frames (the cross-origin player iframe): allow http, https,
 * about, data, blob. Cancel every other scheme (itms-apps:, intent:, market:, custom app schemes) everywhere.
 */
fun isStreamNavigationAllowed(url: String?, isMainFrame: Boolean): Boolean {
    if (url == null) return false
    return when (schemeOf(url)) {
        "http", "https" -> !isMainFrame || isStreamSitePage(url)
        "about" -> !isMainFrame || url.trim().equals("about:blank", ignoreCase = true)
        "data", "blob" -> !isMainFrame
        else -> false
    }
}

/** https://www.youtube.com/watch?v=<url-encoded key>. */
fun youtubeWatchUrl(youtubeKey: String): String = "https://www.youtube.com/watch?v=${youtubeKey.encodeURLParameter()}"
