package com.zobaer53.zedmovies.ui.smallcomponent.mapper

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

internal actual fun LocalDate.formatLocalized(pattern: String): String {
    val date = NSCalendar.currentCalendar.dateFromComponents(toNSDateComponents()) ?: return toString()
    val formatter = NSDateFormatter()
    formatter.locale = NSLocale.currentLocale
    formatter.dateFormat = pattern
    return formatter.stringFromDate(date)
}
