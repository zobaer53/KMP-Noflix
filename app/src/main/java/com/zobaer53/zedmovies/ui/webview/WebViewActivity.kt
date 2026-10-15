package com.zobaer53.zedmovies.ui.webview

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.edsuns.adfilter.AdFilter


class WebViewActivity : ComponentActivity()  {
        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            // Draw edge to edge (including the camera cutout) with system bars hidden.
            WindowCompat.setDecorFitsSystemWindows(window, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            val filter = AdFilter.create(this)
            val type = intent.getStringExtra("type") ?: ""
            val tmdbId = intent.getIntExtra("tmdbId", 0)
            // The stream URL is keyed by TMDB id; without one there is nothing to play.
            if (tmdbId <= 0) {
                finish()
                return
            }
        setContent {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VideoWebView(url = buildStreamUrl(tmdbId, type), lifecycleOwner = this)
                }
        }
    }
}

