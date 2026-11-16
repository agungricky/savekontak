package com.example.wacontactcollector.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val endpoint: String = "",
    val token: String = "",
    val syncEnabled: Boolean = false,
    val intervalHours: Int = 6,
    val checkDeviceContacts: Boolean = true
)

class PreferencesStore(private val context: Context) {
    private object Keys {
        val endpoint = stringPreferencesKey("endpoint")
        val token = stringPreferencesKey("token")
        val syncEnabled = booleanPreferencesKey("sync_enabled")
        val intervalHours = intPreferencesKey("interval_hours")
        val checkDeviceContacts = booleanPreferencesKey("check_device_contacts")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            endpoint = prefs[Keys.endpoint].orEmpty(),
            token = prefs[Keys.token].orEmpty(),
            syncEnabled = prefs[Keys.syncEnabled] ?: false,
            intervalHours = prefs[Keys.intervalHours] ?: 6,
            checkDeviceContacts = prefs[Keys.checkDeviceContacts] ?: true
        )
    }

    suspend fun save(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.endpoint] = settings.endpoint.trim()
            prefs[Keys.token] = settings.token.trim()
            prefs[Keys.syncEnabled] = settings.syncEnabled
            prefs[Keys.intervalHours] = settings.intervalHours
            prefs[Keys.checkDeviceContacts] = settings.checkDeviceContacts
        }
    }
}
