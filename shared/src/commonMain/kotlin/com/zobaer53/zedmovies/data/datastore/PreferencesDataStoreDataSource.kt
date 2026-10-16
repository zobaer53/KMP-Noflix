
package com.zobaer53.zedmovies.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.zobaer53.zedmovies.data.datastore.util.Constants.CONTENT_LANGUAGE_KEY
import com.zobaer53.zedmovies.data.platform.DeviceLocaleProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesDataStoreDataSource(
    private val deviceLocaleProvider: DeviceLocaleProvider,
    private val dataStore: DataStore<Preferences>
) {
    fun getContentLanguage(): Flow<String> = dataStore.data.map { preferences ->
        preferences[CONTENT_LANGUAGE_KEY] ?: run {
            val defaultLanguage = deviceLocaleProvider.language()
            setContentLanguage(defaultLanguage)
            defaultLanguage
        }
    }

    suspend fun setContentLanguage(contentLanguage: String) = dataStore.edit { preferences ->
        preferences[CONTENT_LANGUAGE_KEY] = contentLanguage
    }
}
