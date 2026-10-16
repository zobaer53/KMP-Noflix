package com.zobaer53.zedmovies.ui.smallcomponent.mapper

import com.zobaer53.zedmovies.data.model.ReleaseDate
import kotlinx.datetime.LocalDate

internal fun LocalDate.asReleaseDate() = ReleaseDate(
    fullDate = formatLocalized(FullDatePattern),
    year = year.toString()
)

/**
 * Formats with a java.time-style [pattern] in the device locale. Platform code because
 * kotlinx-datetime's format builder only has fixed (English) month names.
 */
internal expect fun LocalDate.formatLocalized(pattern: String): String

private const val FullDatePattern = "d MMMM, yyyy"
