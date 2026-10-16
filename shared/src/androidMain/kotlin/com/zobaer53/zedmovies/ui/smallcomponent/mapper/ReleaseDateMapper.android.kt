package com.zobaer53.zedmovies.ui.smallcomponent.mapper

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter

internal actual fun LocalDate.formatLocalized(pattern: String): String =
    toJavaLocalDate().format(DateTimeFormatter.ofPattern(pattern))
