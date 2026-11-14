package com.zobaer53.zedmovies.ui.util

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.ISOCountryCodes
import platform.Foundation.ISOLanguageCodes
import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterRoundHalfUp
import platform.Foundation.currentLocale
import platform.Foundation.localizedStringForCountryCode
import platform.Foundation.create
import platform.Foundation.localizedStringForLanguageCode
import platform.Foundation.uppercaseStringWithLocale

// NSNumberFormatter isn't thread-safe and these run during composition, so each call creates its own.

// iOS also lists the 3-letter ISO 639-2 codes; TMDB and the filters sheet use the 2-letter ISO 639-1 ones.
internal actual fun isoLanguageCodes(): List<String> =
    NSLocale.ISOLanguageCodes.filterIsInstance<String>().filter { it.length == 2 }

internal actual fun isoCountryCodes(): List<String> = NSLocale.ISOCountryCodes.filterIsInstance<String>()

internal actual fun displayLanguageName(isoCode: String): String =
    (NSLocale.currentLocale.localizedStringForLanguageCode(isoCode) ?: isoCode).capitalized()

internal actual fun displayCountryName(isoCode: String): String =
    NSLocale.currentLocale.localizedStringForCountryCode(isoCode) ?: isoCode

// Like titlecase(Locale.getDefault()) on Android: the locale decides (Turkish i -> İ).
// The first character is a Unicode scalar, so a surrogate pair stays whole.
@OptIn(BetaInteropApi::class)
internal actual fun String.capitalized(): String {
    if (isEmpty()) return this
    val firstLength = if (this[0].isHighSurrogate() && length > 1) 2 else 1
    val first = NSString.create(string = substring(0, firstLength)).uppercaseStringWithLocale(NSLocale.currentLocale)
    return first + substring(firstLength)
}

internal actual fun formatRating(rating: Double): String {
    val f = NSNumberFormatter()
    f.locale = NSLocale.currentLocale
    f.minimumFractionDigits = 1u
    f.maximumFractionDigits = 1u
    f.usesGroupingSeparator = false
    // The default (half-even) would round 7.25 to 7.2; Android's String.format rounds half up (7.3).
    f.roundingMode = NSNumberFormatterRoundHalfUp
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
