package com.zobaer53.zedmovies.di

import android.content.Context
import com.zobaer53.zedmovies.data.platform.DeviceLocaleProvider
import com.zobaer53.zedmovies.data.platform.RegionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {
    @Provides
    @Singleton
    fun provideRegionProvider(): RegionProvider = AndroidRegionProvider()

    @Provides
    @Singleton
    fun provideDeviceLocaleProvider(@ApplicationContext context: Context): DeviceLocaleProvider =
        AndroidDeviceLocaleProvider(context)
}
