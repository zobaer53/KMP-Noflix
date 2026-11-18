package com.zobaer53.zedmovies.ui.webview

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.zobaer53.zedmovies.ui.playback.STREAM_BASE_URL
import com.zobaer53.zedmovies.ui.playback.STREAM_DOCUMENT_START_SCRIPT
import io.github.edsuns.adfilter.AdFilter

// RestrictedApi: addDocumentStartJavaScript is still marked internal in androidx.webkit 1.4.0;
// the public version needs webkit 1.9+, which requires compileSdk 34.
@SuppressLint("SetJavaScriptEnabled", "RequiresFeature", "RestrictedApi")
@Composable
fun VideoWebView(
    url: String,
    lifecycleOwner: LifecycleOwner,
    tvMode: Boolean = false
) {
    val filter = AdFilter.get()
    val filterViewModel = filter.viewModel
    var rememberUrl by remember { mutableStateOf(url) }
    var isFullScreen by remember { mutableStateOf(false) }
    val activity = LocalActivity.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var pageUrl by remember { mutableStateOf(url) }

    // The D-pad cursor is a fraction of the WebView size, so it keeps its position when the
    // player rotates or the window is resized.
    var cursorX by remember { mutableStateOf(0.5f) }
    var cursorY by remember { mutableStateOf(0.5f) }
    val cursorFocusRequester = remember { FocusRequester() }
    LaunchedEffect(tvMode) { if (tvMode) cursorFocusRequester.requestFocus() }

    // Videos (watch pages and full-screen players) play in landscape; every other page is
    // a phone layout, so it's locked to portrait. TVs have no sensor orientation to lock and
    // their launcher window is already landscape, so orientation changes are skipped entirely.
    fun updateOrientation() {
        if (tvMode) return
        activity?.requestedOrientation = if (customView != null || isStreamWatchUrl(pageUrl)) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    fun hideCustomView() {
        (activity?.window?.decorView as? FrameLayout)?.removeView(customView)
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        isFullScreen = false
        updateOrientation()
    }

    fun dispatchCursorTap() {
        val view = webView ?: return
        val x = cursorX * view.width
        val y = cursorY * view.height
        val now = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(down)
        down.recycle()
        val up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(up)
        up.recycle()
    }

    // The site's player lives in a cross-origin iframe, so key focus cannot drive it reliably.
    // Moving a pointer and synthesising a tap works whatever the DOM looks like.
    fun onTvKey(event: androidx.compose.ui.input.key.KeyEvent): Boolean {
        if (!tvMode || event.type != KeyEventType.KeyDown) return false
        return when (event.key) {
            Key.DirectionLeft -> { cursorX = (cursorX - CursorStep).coerceIn(0f, 1f); true }
            Key.DirectionRight -> { cursorX = (cursorX + CursorStep).coerceIn(0f, 1f); true }
            Key.DirectionUp -> { cursorY = (cursorY - CursorStep).coerceIn(0f, 1f); true }
            Key.DirectionDown -> { cursorY = (cursorY + CursorStep).coerceIn(0f, 1f); true }
            Key.Enter, Key.DirectionCenter -> { dispatchCursorTap(); true }
            Key.Back -> {
                when {
                    customView != null -> hideCustomView()
                    webView?.canGoBack() == true -> webView?.goBack()
                    else -> activity?.finish()
                }
                true
            }
            else -> false
        }
    }

    BackHandler {
        when {
            customView != null -> hideCustomView()
            webView?.canGoBack() == true -> webView?.goBack()
            else -> activity?.finish()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(cursorFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event -> onTvKey(event) }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                    WebView.setWebContentsDebuggingEnabled(true)
                }
                WebView(context).apply {
                    filter.setupWebView(this)
                    // Add filter list subscriptions on first installation.
                    if (!filter.hasInstallation) {
                        val map = mapOf(
                            "AdGuard Base" to "https://filters.adtidy.org/extension/chromium/filters/2.txt",
                            "EasyPrivacy Lite" to "https://filters.adtidy.org/extension/chromium/filters/118_optimized.txt",
                            "AdGuard Tracking Protection" to "https://filters.adtidy.org/extension/chromium/filters/3.txt",
                            "AdGuard Annoyances" to "https://filters.adtidy.org/extension/chromium/filters/14.txt",
                            "AdGuard Chinese" to "https://filters.adtidy.org/extension/chromium/filters/224.txt",
                            "NoCoin Filter List" to "https://filters.adtidy.org/extension/chromium/filters/242.txt"
                        )
                        for ((key, value) in map) {
                            val subscription = filterViewModel.addFilter(key, value)
                            filterViewModel.download(subscription.id)
                        }
                    }

                    filterViewModel.onDirty.observe(lifecycleOwner) {
                        // Clear cache when there are changes to the filter.
                        // You need to refresh the page manually to make the changes take effect.
                        clearCache(false)
                    }

                    // Enable JavaScript and other settings
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        // Never auto-darken the site (setForceDark is a no-op from targetSdk 33).
                        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                            WebSettingsCompat.setAlgorithmicDarkeningAllowed(this, false)
                        }
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                        WebViewCompat.addDocumentStartJavaScript(
                            this, STREAM_DOCUMENT_START_SCRIPT, setOf(STREAM_BASE_URL)
                        )
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            val decor = activity?.window?.decorView as? FrameLayout
                            if (view == null || decor == null || customView != null) {
                                callback?.onCustomViewHidden()
                                return
                            }
                            // Enter fullscreen mode here
                            decor.addView(
                                view,
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            )
                            customView = view
                            customViewCallback = callback
                            isFullScreen = true
                            updateOrientation()
                        }

                        override fun onHideCustomView() {
                            // Exit fullscreen mode here
                            hideCustomView()
                        }
                    }

                    webViewClient = MyWebClient { newUrl ->
                        pageUrl = newUrl
                        updateOrientation()
                    }
                    webView = this

                    // Load the video URL
                    loadUrl(rememberUrl)
                }
            }
        )

        if (tvMode) {
            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * cursorX - CursorSize / 2,
                        y = maxHeight * cursorY - CursorSize / 2
                    )
                    .size(CursorSize)
                    .clip(CircleShape)
                    .background(color = Color.White.copy(alpha = CursorAlpha))
            )
        }
    }
}

private val CursorSize = 20.dp
private const val CursorStep = 0.06f
private const val CursorAlpha = 0.75f
