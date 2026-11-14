package com.zobaer53.zedmovies.data.platform

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages

internal class IosRegionProvider : RegionProvider {
    override fun region(): String = NSLocale.currentLocale.countryCode ?: ""
}

/**
 * The first language of the system list (Settings > General > Language & Region), as a 2-letter code.
 * NSBundle.preferredLocalizations would only know the app's own localizations ("en"). Hebrew is `he` here,
 * `iw` on Android; TMDB accepts both.
 */
internal class IosDeviceLocaleProvider : DeviceLocaleProvider {
    override fun language(): String {
        val first = NSLocale.preferredLanguages.firstOrNull() as? String ?: return NSLocale.currentLocale.languageCode
        return NSLocale(localeIdentifier = first).languageCode
    }
}
