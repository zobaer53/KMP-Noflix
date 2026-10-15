package com.zobaer53.zedmovies.data.platform

/** The device's primary language, as an ISO 639-1 code. Android: the first locale of the configuration. */
interface DeviceLocaleProvider {
    fun language(): String
}
