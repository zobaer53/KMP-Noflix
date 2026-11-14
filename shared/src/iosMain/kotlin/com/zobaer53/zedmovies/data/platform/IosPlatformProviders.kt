package com.zobaer53.zedmovies.data.platform

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages

internal class IosRegionProvider : RegionProvider {
    override fun region(): String = NSLocale.currentLocale.countryCode ?: ""
}

internal class IosDeviceLocaleProvider : DeviceLocaleProvider {
    override fun language(): String {
        val first = NSLocale.preferredLanguages.firstOrNull() as? String ?: return NSLocale.currentLocale.languageCode
        return NSLocale(localeIdentifier = first).languageCode
    }
}
