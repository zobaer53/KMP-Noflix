package com.zobaer53.zedmovies.ui.smallcomponent.mapper

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

// A Gregorian calendar for both the date and the formatter: java.time always formats in ISO, while the
// user's calendar (Japanese, Buddhist, …) would shift the year. The formatter is created per call:
// NSDateFormatter is expensive but not thread-safe, and this runs during composition.
internal actual fun LocalDate.formatLocalized(pattern: String): String {
    val gregorian = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
    val date = gregorian.dateFromComponents(toNSDateComponents()) ?: return toString()
    val formatter = NSDateFormatter()
    formatter.calendar = gregorian
    formatter.locale = NSLocale.currentLocale
    formatter.dateFormat = pattern
    return formatter.stringFromDate(date)
}
