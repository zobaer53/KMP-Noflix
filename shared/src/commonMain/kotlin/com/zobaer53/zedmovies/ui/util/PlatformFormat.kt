package com.zobaer53.zedmovies.ui.util

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

// Formatting that needs a platform implementation (the locale data and number formats live in the
// platform libraries). The actuals are in PlatformFormat.android.kt and PlatformFormat.jvm.kt.

internal fun currentYear(): Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).year

/** Every ISO 639-1 language code. */
internal expect fun isoLanguageCodes(): List<String>

/** Every ISO 3166-1 country code. */
internal expect fun isoCountryCodes(): List<String>

/** The language's name in the device language, capitalized. */
internal expect fun displayLanguageName(isoCode: String): String

/** The country's name in the device language, or the code itself if it isn't a valid region. */
internal expect fun displayCountryName(isoCode: String): String

/** Upper-cases the first letter with the device locale's rules. */
internal expect fun String.capitalized(): String

internal expect fun formatRating(rating: Double): String

internal expect fun formatCount(value: Int): String

/**
 * An integer in the device locale's digits, without grouping (years stay "2020"). Compose resources
 * substitute %d with toString(), so Int arguments are formatted here and passed as %s.
 */
internal expect fun formatInteger(value: Int): String
