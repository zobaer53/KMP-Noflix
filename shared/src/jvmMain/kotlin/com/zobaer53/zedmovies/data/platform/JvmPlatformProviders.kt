package com.zobaer53.zedmovies.data.platform

import java.util.Locale

// JVM implementations, for the host tests (the jvm target is never shipped).

internal class JvmRegionProvider : RegionProvider {
    override fun region(): String = Locale.getDefault().country
}

internal class JvmDeviceLocaleProvider : DeviceLocaleProvider {
    override fun language(): String = Locale.getDefault().language
}
