package com.privacypal.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import app.cash.turbine.test
import com.privacypal.app.domain.model.ThemeMode
import com.privacypal.app.domain.model.ThemeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // Okio storage: the default FileStorage's File.renameTo cannot replace files on Windows hosts.
    private fun TestScope.newDataStore(scope: CoroutineScope = backgroundScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) {
                File(tmp.root, "settings.preferences_pb").absolutePath.toPath()
            },
            scope = scope,
        )

    @Test
    fun emptyStoreGivesDefaults() = runTest {
        val repository = DataStoreSettingsRepository(newDataStore())

        assertEquals(ThemeSettings(ThemeMode.SYSTEM, dynamicColor = true), repository.themeSettings.first())
    }

    @Test
    fun everyThemeModeRoundTrips() = runTest {
        val repository = DataStoreSettingsRepository(newDataStore())

        repository.themeSettings.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)
            repository.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)
            repository.setThemeMode(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, awaitItem().themeMode)
            repository.setThemeMode(ThemeMode.SYSTEM)
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)
        }
    }

    @Test
    fun dynamicColorToggles() = runTest {
        val repository = DataStoreSettingsRepository(newDataStore())

        repository.themeSettings.test {
            assertEquals(true, awaitItem().dynamicColor)
            repository.setDynamicColor(false)
            assertEquals(ThemeSettings(ThemeMode.SYSTEM, dynamicColor = false), awaitItem())
            repository.setDynamicColor(true)
            assertEquals(true, awaitItem().dynamicColor)
        }
    }

    @Test
    fun settingSameValueDoesNotEmitAgain() = runTest {
        val repository = DataStoreSettingsRepository(newDataStore())

        repository.themeSettings.test {
            awaitItem()
            repository.setThemeMode(ThemeMode.SYSTEM)
            repository.setDynamicColor(true)
            repository.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)
        }
    }

    @Test
    fun unknownStoredThemeModeFallsBackToSystem() = runTest {
        val dataStore = newDataStore()
        dataStore.edit { it[DataStoreSettingsRepository.KEY_THEME_MODE] = "SEPIA" }

        val repository = DataStoreSettingsRepository(dataStore)

        assertEquals(ThemeMode.SYSTEM, repository.themeSettings.first().themeMode)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun valuesPersistAcrossInstances() = runTest {
        // Simulates process death: the first DataStore's scope dies, a new one reads the same file.
        val firstScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        DataStoreSettingsRepository(newDataStore(firstScope)).apply {
            setThemeMode(ThemeMode.DARK)
            setDynamicColor(false)
        }
        firstScope.cancel()
        advanceUntilIdle()

        val reopened = DataStoreSettingsRepository(newDataStore())

        assertEquals(ThemeSettings(ThemeMode.DARK, dynamicColor = false), reopened.themeSettings.first())
    }
}
