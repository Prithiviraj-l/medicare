package com.example.medicare.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

private val Context.dataStore by preferencesDataStore(name = "settings_prefs")

class PreferenceStore(private val context: Context) {

    companion object {
        val RINGTONE_URI_KEY = stringPreferencesKey("ringtone_uri")
        val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
    }

    val ringtoneUri: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[RINGTONE_URI_KEY] ?: ""
        }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[VIBRATION_ENABLED_KEY] ?: true
        }

    suspend fun saveRingtoneUri(uri: String) {
        context.dataStore.edit { settings ->
            settings[RINGTONE_URI_KEY] = uri
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[VIBRATION_ENABLED_KEY] = enabled
        }
    }
}
