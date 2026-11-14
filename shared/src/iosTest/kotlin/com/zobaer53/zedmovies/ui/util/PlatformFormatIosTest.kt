package com.zobaer53.zedmovies.ui.util

import com.zobaer53.zedmovies.ui.smallcomponent.mapper.formatLocalized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class PlatformFormatIosTest {
    @Test
    fun ratingRoundsHalfUp() {
        // 7.25 is exact in binary: half-up gives 7.3 like Android's String.format, half-even would give 7.2.
        assertTrue(formatRating(7.25).endsWith("3"), formatRating(7.25))
        assertTrue(formatRating(9.0).endsWith("0"), formatRating(9.0))
    }

    @Test
    fun integerHasNoGroupingSeparator() {
        val formatted = formatInteger(2020)
        assertEquals(4, formatted.length, formatted)
        assertTrue(formatted.none { it.isWhitespace() || it == ',' || it == '.' }, formatted)
    }

    @Test
    fun countHasAGroupingSeparator() {
        val formatted = formatCount(12345)
        assertTrue(formatted.length > 5, formatted)
    }

    @Test
    fun languageCodesAreTwoLettersAndIncludeEnglishAndGerman() {
        val codes = isoLanguageCodes()
        assertTrue(codes.isNotEmpty())
        assertTrue(codes.all { it.length == 2 }, codes.filter { it.length != 2 }.toString())
        assertTrue("en" in codes)
        assertTrue("de" in codes)
    }

    @Test
    fun countryCodesIncludeTheUnitedStates() {
        assertTrue("US" in isoCountryCodes())
    }

    @Test
    fun namesAreNeverEmpty() {
        assertTrue(displayCountryName("ZZ").isNotEmpty())
        assertTrue(displayCountryName("US").isNotEmpty())
        assertTrue(displayLanguageName("xx").isNotEmpty())
        assertTrue(displayLanguageName("de").isNotEmpty())
    }

    @Test
    fun capitalizedUppercasesTheFirstCharacterOnly() {
        assertEquals("Zed movies", "zed movies".capitalized())
        assertEquals("", "".capitalized())
        assertEquals("Z", "z".capitalized())
    }

    @Test
    fun releaseDateContainsDayAndYear() {
        val formatted = LocalDate(2026, 7, 29).formatLocalized("d MMMM, yyyy")
        assertTrue("29" in formatted, formatted)
        assertTrue("2026" in formatted, formatted)
    }
}
