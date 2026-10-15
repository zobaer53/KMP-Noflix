package com.zobaer53.zedmovies.di

import android.content.Context
import androidx.core.os.ConfigurationCompat
import com.zobaer53.zedmovies.data.platform.DeviceLocaleProvider
import com.zobaer53.zedmovies.data.platform.RegionProvider
import java.util.Locale

// Android implementations of the data layer's platform providers. They move to androidMain in Phase 3.

class AndroidRegionProvider : RegionProvider {
    override fun region(): String = Locale.getDefault().country
}

class AndroidDeviceLocaleProvider(private val context: Context) : DeviceLocaleProvider {
    override fun language(): String {
        val locale = checkNotNull(ConfigurationCompat.getLocales(context.resources.configuration)[0])
        return locale.language
    }
}
