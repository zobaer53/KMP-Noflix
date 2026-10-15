package com.zobaer53.zedmovies.ui.screenshot

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.zobaer53.zedmovies.ui.designsystem.component.LocalSnackbarHostState
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme
import com.zobaer53.zedmovies.ui.playback.LocalMediaLauncher
import com.zobaer53.zedmovies.ui.playback.MediaLauncher
import com.zobaer53.zedmovies.ui.playback.PlayRequest
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders a screen's stateless composable with fake UI state on the JVM and compares it with the
 * committed image in `app/src/test/screenshots/` (the "Visual" success criterion, see
 * doc/kmp-android-plan.md, B3).
 *
 * - `./gradlew :app:recordRoborazziDebug` records new reference images.
 * - `./gradlew :app:verifyRoborazziDebug` fails on any difference and writes `*_compare.png` diffs to
 *   `app/build/outputs/roborazzi/`.
 * - A plain `testDebugUnitTest` only renders, so it also catches crashes.
 *
 * Images use a Pixel 6a sized screen in the dark theme (the theme follows the system's night mode),
 * a plain [Application] so no Hilt graph is built, and a fixed clock: animations (shimmer
 * placeholders, progress indicators) stop at the same frame on every run.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// SDK 35: Robolectric's SDK 36 runtime needs Java 21, and the build runs on Java 17.
@Config(sdk = [35], qualifiers = "w411dp-h914dp-night-420dpi", application = Application::class)
abstract class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val testName = TestName()

    /** Sets [content] inside the app theme and the composition locals the app shell provides. */
    protected fun setScreen(content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            zedMoviesTheme {
                CompositionLocalProvider(
                    LocalMediaLauncher provides NoOpMediaLauncher,
                    LocalSnackbarHostState provides remember { SnackbarHostState() }
                ) {
                    // The app's Scaffold draws this background behind every screen.
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        content()
                    }
                }
            }
        }
        settle()
    }

    /**
     * Advances the fixed clock far enough for layout and enter animations to finish, idling the
     * main looper between steps: work posted there (paging, opening the filter sheet) can start
     * new animations that need the clock again.
     */
    protected fun settle() {
        repeat(SettleSteps) {
            composeRule.waitForIdle()
            composeRule.mainClock.advanceTimeBy(SettleStepMillis)
        }
        composeRule.waitForIdle()
    }

    /** Captures the screen's window, named after the test method. */
    protected fun capture(name: String = testName.methodName) =
        composeRule.onRoot().captureRoboImage("$ScreenshotDir/$name.png")

    /** Captures every window, for content in a dialog or bottom sheet. */
    @OptIn(ExperimentalRoborazziApi::class)
    protected fun captureWithDialogs(name: String = testName.methodName) =
        captureScreenRoboImage("$ScreenshotDir/$name.png")

    private object NoOpMediaLauncher : MediaLauncher {
        override fun play(request: PlayRequest) = Unit
        override fun playTrailer(youtubeKey: String) = Unit
    }

    private companion object {
        // Relative to the module: unit tests run with app/ as the working directory.
        const val ScreenshotDir = "src/test/screenshots"
        const val SettleSteps = 3
        const val SettleStepMillis = 1_000L
    }
}
