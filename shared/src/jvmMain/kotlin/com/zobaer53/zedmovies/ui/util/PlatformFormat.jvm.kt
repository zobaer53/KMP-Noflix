package com.zobaer53.zedmovies.ui.util

import java.text.NumberFormat
import java.util.Locale

internal actual fun isoLanguageCodes(): List<String> = Locale.getISOLanguages().asList()

internal actual fun isoCountryCodes(): List<String> = Locale.getISOCountries().asList()

internal actual fun displayLanguageName(isoCode: String): String =
    Locale.forLanguageTag(isoCode).getDisplayLanguage(Locale.getDefault()).capitalized()

internal actual fun displayCountryName(isoCode: String): String =
    runCatching { Locale.Builder().setRegion(isoCode).build().getDisplayCountry(Locale.getDefault()) }
        .getOrDefault(isoCode)

internal actual fun String.capitalized(): String = replaceFirstChar { it.titlecase(Locale.getDefault()) }

internal actual fun formatRating(rating: Double): String = String.format(Locale.getDefault(), "%.1f", rating)

internal actual fun formatCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)

internal actual fun formatInteger(value: Int): String = String.format(Locale.getDefault(), "%d", value)
