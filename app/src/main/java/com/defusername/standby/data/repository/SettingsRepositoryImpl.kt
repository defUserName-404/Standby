package com.defusername.standby.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.defusername.standby.data.local.SettingsSerializer
import com.defusername.standby.domain.model.AppSettings
import com.defusername.standby.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")
private val SETTINGS_KEY = stringPreferencesKey("app_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    override val settings: Flow<AppSettings> = context.dataStore.data
        .map { prefs ->
            prefs[SETTINGS_KEY]?.let { json ->
                try {
                    Json.decodeFromString<AppSettings>(json)
                } catch (_: Exception) {
                    SettingsSerializer.defaultValue
                }
            } ?: SettingsSerializer.defaultValue
        }
        .distinctUntilChanged()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs[SETTINGS_KEY]?.let { json ->
                try {
                    Json.decodeFromString<AppSettings>(json)
                } catch (_: Exception) {
                    SettingsSerializer.defaultValue
                }
            } ?: SettingsSerializer.defaultValue
            prefs[SETTINGS_KEY] = Json.encodeToString(AppSettings.serializer(), transform(current))
        }
    }
}
