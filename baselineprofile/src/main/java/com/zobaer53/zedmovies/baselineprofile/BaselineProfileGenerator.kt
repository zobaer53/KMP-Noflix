package com.zobaer53.zedmovies.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code the app runs on its main journeys into :app's Baseline Profile
 * (app/src/release/generated/baselineProfiles/). Run with ./gradlew :app:generateBaselineProfile.
 * The profile goes into the APK, so ART compiles that code ahead of time at install instead of
 * interpreting it on the first launches (Compose ships with the app and isn't precompiled otherwise).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE_NAME,
        // Startup code also goes into the startup profile, which lays out the dex for faster loading.
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        waitForHome()
        browseHome()
        openDetails()
        browseSearchAndWishlist()
    }
}
