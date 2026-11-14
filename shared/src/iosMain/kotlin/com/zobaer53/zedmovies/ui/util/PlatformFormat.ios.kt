package com.zobaer53.zedmovies.ui.util

import platform.Foundation.ISOCountryCodes
import platform.Foundation.ISOLanguageCodes
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.currentLocale
import platform.Foundation.localizedStringForCountryCode
import platform.Foundation.localizedStringForLanguageCode

internal actual fun isoLanguageCodes(): List<String> = NSLocale.ISOLanguageCodes.filterIsInstance<String>().filter { it.length == 2 }

internal actual fun isoCountryCodes(): List<String> = NSLocale.ISOCountryCodes.filterIsInstance<String>()

internal actual fun displayLanguageName(isoCode: String): String =
    (NSLocale.currentLocale.localizedStringForLanguageCode(isoCode) ?: isoCode).capitalized()

internal actual fun displayCountryName(isoCode: String): String =
    NSLocale.currentLocale.localizedStringForCountryCode(isoCode) ?: isoCode

internal actual fun String.capitalized(): String = replaceFirstChar { it.titlecase() }

internal actual fun formatRating(rating: Double): String {
    val f = NSNumberFormatter()
    f.locale = NSLocale.currentLocale
    f.minimumFractionDigits = 1u
    f.maximumFractionDigits = 1u
    f.usesGroupingSeparator = false
    return f.stringFromNumber(NSNumber(rating)) ?: rating.toString()
}

internal actual fun formatCount(value: Int): String {
    val f = NSNumberFormatter()
    f.locale = NSLocale.currentLocale
    f.numberStyle = NSNumberFormatterDecimalStyle
    f.maximumFractionDigits = 0u
    return f.stringFromNumber(NSNumber(value)) ?: value.toString()
}

internal actual fun formatInteger(value: Int): String {
    val f = NSNumberFormatter()
    f.locale = NSLocale.currentLocale
    f.usesGroupingSeparator = false
    return f.stringFromNumber(NSNumber(value)) ?: value.toString()
}
