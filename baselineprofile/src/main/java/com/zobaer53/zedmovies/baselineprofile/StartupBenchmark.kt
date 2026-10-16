package com.zobaer53.zedmovies.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold start of the release app (R8 on) without and with the Baseline Profile. Run with
 * ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest; the results land in
 * baselineprofile/build/outputs/connected_android_test_additional_output/.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    /** Nothing precompiled: what a sideloaded install got before B7 until ART's background dexopt ran. */
    @Test
    fun startupWithoutProfile() = startup(CompilationMode.None())

    /** The Baseline Profile compiled ahead of time, as profileinstaller and Play install it. */
    @Test
    fun startupWithProfile() = startup(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.COLD,
        iterations = 10,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
    }
}
