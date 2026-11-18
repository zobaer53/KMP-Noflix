
package com.zobaer53.zedmovies

import android.app.UiModeManager
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.graphics.Color
import android.os.Build
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.zobaer53.zedmovies.playback.AndroidMediaLauncher
import com.zobaer53.zedmovies.ui.adaptive.FormFactor
import com.zobaer53.zedmovies.ui.adaptive.InputMode
import com.zobaer53.zedmovies.ui.playback.LocalMediaLauncher
import com.zobaer53.zedmovies.ui.zedMoviesApp

class zedMoviesActivity : ComponentActivity() {
    // The Activity itself as context: Play and Trailer start their activities without NEW_TASK.
    private val mediaLauncher = AndroidMediaLauncher(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // TV uses the wide, D-pad layout from Phase 5. Phones keep the old portrait lock, now at
        // runtime, so the manifest no longer declares a required portrait screen feature (which
        // would make the app invisible in the TV launcher).
        val uiModeManager = getSystemService(UiModeManager::class.java)
        val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        if (!isTv) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // No scrim behind 3-button navigation (API < 35): the app's background shows through,
        // as before edge-to-edge. Icon colors are set by SystemBarsAppearance.
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            CompositionLocalProvider(LocalMediaLauncher provides mediaLauncher) {
                zedMoviesApp(
                    formFactor = if (isTv) FormFactor.Tv else null,
                    inputMode = if (isTv) InputMode.Directional else null
                )
            }
        }
    }
}
