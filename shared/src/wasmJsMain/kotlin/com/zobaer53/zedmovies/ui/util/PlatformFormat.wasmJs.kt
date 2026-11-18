package com.zobaer53.zedmovies.ui.util

// Browsers have no java.util.Locale; v1 shows ISO codes and uses the browser-neutral numeric
// formatting. The list filters still work because their values are the same ISO codes.
private val languageCodes = listOf(
    "af", "am", "ar", "az", "be", "bg", "bn", "ca", "cs", "da", "de", "el", "en", "es", "et", "fa",
    "fi", "fr", "gl", "he", "hi", "hr", "hu", "hy", "id", "is", "it", "ja", "ka", "kk", "km", "kn",
    "ko", "ky", "lo", "lt", "lv", "mk", "ml", "mn", "mr", "ms", "my", "ne", "nl", "no", "or", "pa",
    "pl", "pt", "ro", "ru", "si", "sk", "sl", "sq", "sr", "sv", "sw", "ta", "te", "th", "tl", "tr",
    "uk", "ur", "uz", "vi", "zh", "zu"
)

private val countryCodes = listOf(
    "AE", "AR", "AT", "AU", "BE", "BG", "BR", "CA", "CH", "CL", "CN", "CO", "CZ", "DE", "DK", "EG",
    "ES", "FI", "FR", "GB", "GR", "HK", "HU", "ID", "IE", "IL", "IN", "IT", "JP", "KE", "KR", "MX",
    "MY", "NG", "NL", "NO", "NZ", "PH", "PL", "PT", "RO", "RU", "SA", "SE", "SG", "TH", "TR", "TW",
    "UA", "US", "VN", "ZA"
)

internal actual fun isoLanguageCodes(): List<String> = languageCodes

internal actual fun isoCountryCodes(): List<String> = countryCodes

internal actual fun displayLanguageName(isoCode: String): String = isoCode.capitalized()

internal actual fun displayCountryName(isoCode: String): String = isoCode

internal actual fun String.capitalized(): String = replaceFirstChar { it.uppercaseChar() }

internal actual fun formatRating(rating: Double): String =
    if (rating == rating.toLong().toDouble()) "$rating.0" else rating.toString()

internal actual fun formatCount(value: Int): String = value.toString()

internal actual fun formatInteger(value: Int): String = value.toString()
