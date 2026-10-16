package com.zobaer53.zedmovies.data.platform

import android.content.Context
import java.util.Locale

// Android implementations of the data layer's platform providers.

internal class AndroidRegionProvider : RegionProvider {
    override fun region(): String = Locale.getDefault().country
}

internal class AndroidDeviceLocaleProvider(private val context: Context) : DeviceLocaleProvider {
    // The configuration's first locale (what ConfigurationCompat.getLocales returns on API 24+).
    override fun language(): String {
        val locale = checkNotNull(context.resources.configuration.locales[0])
        return locale.language
    }
}
