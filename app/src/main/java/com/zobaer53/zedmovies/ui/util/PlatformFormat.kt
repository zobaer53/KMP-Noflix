package com.zobaer53.zedmovies.ui.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

// Formatting that needs a different implementation per platform (java.util/java.text here).
// These become expect/actual functions when the UI moves to commonMain (Phase 4).

internal fun currentYear(): Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).year

/** Every ISO 639-1 language code. */
internal fun isoLanguageCodes(): List<String> = Locale.getISOLanguages().asList()

/** Every ISO 3166-1 country code. */
internal fun isoCountryCodes(): List<String> = Locale.getISOCountries().asList()

/** The language's name in the device language, capitalized. */
internal fun displayLanguageName(isoCode: String): String =
    Locale.forLanguageTag(isoCode).getDisplayLanguage(Locale.getDefault()).capitalized()

/** The country's name in the device language, or the code itself if it isn't a valid region. */
internal fun displayCountryName(isoCode: String): String =
    runCatching { Locale.Builder().setRegion(isoCode).build().getDisplayCountry(Locale.getDefault()) }
        .getOrDefault(isoCode)

/** Upper-cases the first letter with the device locale's rules. */
internal fun String.capitalized(): String = replaceFirstChar { it.titlecase(Locale.getDefault()) }

internal fun formatRating(rating: Double): String = String.format(Locale.getDefault(), "%.1f", rating)

internal fun formatCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)
