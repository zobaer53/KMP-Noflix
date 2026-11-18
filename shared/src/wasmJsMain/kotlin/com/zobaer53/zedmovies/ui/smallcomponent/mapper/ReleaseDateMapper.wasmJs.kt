package com.zobaer53.zedmovies.ui.smallcomponent.mapper

import kotlinx.datetime.LocalDate

// The browser has no java.time formatter; v1 uses the ISO date. The pattern argument is kept so
// the shared signature stays identical on every platform.
internal actual fun LocalDate.formatLocalized(pattern: String): String = toString()
