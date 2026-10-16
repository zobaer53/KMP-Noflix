package com.zobaer53.zedmovies.data.platform

/** The device's region, as an ISO 3166-1 country code (may be empty). Android: `Locale.getDefault().country`. */
interface RegionProvider {
    fun region(): String
}
