
package com.zobaer53.zedmovies.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import com.zobaer53.zedmovies.ui.adaptive.FormFactor
import com.zobaer53.zedmovies.ui.adaptive.InputMode
import com.zobaer53.zedmovies.ui.adaptive.LocalFormFactor
import com.zobaer53.zedmovies.ui.adaptive.LocalInputMode
import com.zobaer53.zedmovies.ui.designsystem.component.LocalSnackbarHostState
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme
import com.zobaer53.zedmovies.ui.home.HomeScreen
import com.zobaer53.zedmovies.ui.home.HomeUiState
import com.zobaer53.zedmovies.ui.playback.LocalMediaLauncher
import com.zobaer53.zedmovies.ui.playback.MediaLauncher
import com.zobaer53.zedmovies.ui.playback.PlayRequest
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Phase 5: the compact layout is untouched, while keyboard/D-pad hosts get a focusable refresh
 * action. Rendered on Compose Desktop so no Android device is needed.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class AdaptiveUiTest {

    private val defaultLocale = Locale.getDefault()

    @BeforeTest
    fun setUp() {
        Locale.setDefault(Locale.US)
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun phoneHomeKeepsTheTouchOnlyLayout() = renderHome(FormFactor.Phone, InputMode.Touch) {
        onAllNodesWithText("Refresh", ignoreCase = true).assertCountEquals(0)
    }

    @Test
    fun desktopHomeShowsTheKeyboardRefreshAction() = renderHome(FormFactor.Desktop, InputMode.Pointer) {
        onAllNodesWithText("Refresh", ignoreCase = true).assertCountEquals(1)
    }

    private fun renderHome(
        formFactor: FormFactor,
        inputMode: InputMode,
        checks: ComposeUiTest.() -> Unit
    ) = runComposeUiTest {
        setContent {
            zedMoviesTheme(darkTheme = true) {
                CompositionLocalProvider(
                    LocalFormFactor provides formFactor,
                    LocalInputMode provides inputMode,
                    LocalMediaLauncher provides NoOpMediaLauncher,
                    LocalSnackbarHostState provides remember { SnackbarHostState() }
                ) {
                    HomeScreen(
                        uiState = HomeUiState(
                            movies = smokeHomeMovies(),
                            tvShows = smokeHomeTvShows()
                        ),
                        onSeeAllClick = {},
                        onMovieClick = {},
                        onTvShowClick = {},
                        onRefresh = {},
                        onRetry = {},
                        onOfflineModeClick = {}
                    )
                }
            }
        }
        waitForIdle()
        checks()
    }

    private object NoOpMediaLauncher : MediaLauncher {
        override fun play(request: PlayRequest) = Unit
        override fun playTrailer(youtubeKey: String) = Unit
    }
}
