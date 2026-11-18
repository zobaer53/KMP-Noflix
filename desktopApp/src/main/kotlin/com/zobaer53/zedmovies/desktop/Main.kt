package com.zobaer53.zedmovies.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import com.zobaer53.zedmovies.di.sharedModules
import com.zobaer53.zedmovies.di.viewModelModule
import com.zobaer53.zedmovies.ui.adaptive.FormFactor
import com.zobaer53.zedmovies.ui.adaptive.InputMode
import com.zobaer53.zedmovies.ui.playback.LocalMediaLauncher
import com.zobaer53.zedmovies.ui.zedMoviesApp
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File

fun main() = application {
    // Room and DataStore live in the OS app-data folder (see PlatformModule.jvm).
    System.setProperty("noflix.data.dir", desktopDataDirectory().absolutePath)
    startKoin {
        modules(
            module {
                single<zedMoviesApiKeyProvider> {
                    object : zedMoviesApiKeyProvider {
                        override val apiKey: String? = DESKTOP_API_KEY.ifBlank { null }
                    }
                }
            } + viewModelModule + sharedModules
        )
    }
    Window(onCloseRequest = ::exitApplication, title = "NoFlix") {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Playback opens in your browser. NoFlix cannot block ads inside another site's player — use a browser ad blocker.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                CompositionLocalProvider(LocalMediaLauncher provides DesktopMediaLauncher) {
                    zedMoviesApp(formFactor = FormFactor.Desktop, inputMode = InputMode.Pointer)
                }
            }
        }
    }
}

private fun desktopDataDirectory(): File {
    val home = System.getProperty("user.home")
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("mac") -> File(home, "Library/Application Support/NoFlix")
        os.contains("win") -> File(System.getenv("APPDATA") ?: home, "NoFlix")
        else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "noflix")
    }.also { it.mkdirs() }
}
