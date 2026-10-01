package com.burton.weather.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.weatherStore: DataStore<Preferences> by preferencesDataStore(name = "burton_weather")

@Singleton
class LocalPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val catalog = stringPreferencesKey("catalog")
    }

    suspend fun load(): StoredState {
        val json = context.weatherStore.data.map { it[Keys.catalog].orEmpty() }.first()
        return CityCodec.decode(json)
    }

    suspend fun save(state: StoredState) {
        context.weatherStore.edit { prefs ->
            prefs[Keys.catalog] = CityCodec.encode(state)
        }
    }
}
