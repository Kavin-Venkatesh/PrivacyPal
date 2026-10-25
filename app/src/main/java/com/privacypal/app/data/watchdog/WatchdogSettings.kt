package com.privacypal.app.data.watchdog

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface WatchdogSettingsRepository {
    /** Whether the user turned on "Watch mic and camera". Off by default. */
    val enabled: Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)
}

class DataStoreWatchdogSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    WatchdogSettingsRepository {

    override val enabled: Flow<Boolean> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it[KEY_ENABLED] ?: false }
        .distinctUntilChanged()

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("watchdog_enabled")
    }
}
