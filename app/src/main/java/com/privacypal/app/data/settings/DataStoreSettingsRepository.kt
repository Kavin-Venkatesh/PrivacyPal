package com.privacypal.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.privacypal.app.domain.model.ThemeMode
import com.privacypal.app.domain.model.ThemeSettings
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    SettingsRepository {

    override val themeSettings: Flow<ThemeSettings> = dataStore.data
        .catch { error ->
            // An unreadable file must not crash the app; fall back to defaults.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            ThemeSettings(
                themeMode = prefs[KEY_THEME_MODE].toThemeMode(),
                dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: ThemeSettings().dynamicColor,
            )
        }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    internal companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

        private fun String?.toThemeMode(): ThemeMode =
            ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM
    }
}
