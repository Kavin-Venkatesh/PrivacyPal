package com.privacypal.app.data.watchdog

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.privacypal.app.domain.watchdog.HistoryRetention
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

    /** How many days of mic and camera history to keep; one of [HistoryRetention.OPTIONS_DAYS]. */
    val retentionDays: Flow<Int>

    suspend fun setRetentionDays(days: Int)
}

class DataStoreWatchdogSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    WatchdogSettingsRepository {

    private val prefs = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

    override val enabled: Flow<Boolean> = prefs
        .map { it[KEY_ENABLED] ?: false }
        .distinctUntilChanged()

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    override val retentionDays: Flow<Int> = prefs
        .map { prefs ->
            prefs[KEY_RETENTION_DAYS]?.takeIf { it in HistoryRetention.OPTIONS_DAYS }
                ?: HistoryRetention.DEFAULT_DAYS
        }
        .distinctUntilChanged()

    override suspend fun setRetentionDays(days: Int) {
        require(days in HistoryRetention.OPTIONS_DAYS) { "Unsupported retention: $days" }
        dataStore.edit { it[KEY_RETENTION_DAYS] = days }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("watchdog_enabled")
        val KEY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
    }
}
