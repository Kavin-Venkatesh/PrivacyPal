package com.privacypal.app.ui.settings

import app.cash.turbine.test
import com.privacypal.app.domain.model.ThemeMode
import com.privacypal.app.domain.model.ThemeSettings
import com.privacypal.app.testing.FakeSettingsRepository
import com.privacypal.app.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository()

    @Test
    fun nullUntilSettingsLoad() = runTest {
        val viewModel = SettingsViewModel(repository) { false }

        viewModel.themeSettings.test {
            assertNull(awaitItem())
            repository.emit(ThemeSettings())
            assertEquals(ThemeSettings(), awaitItem())
        }
    }

    @Test
    fun selectingThemeModeUpdatesState() = runTest {
        repository.emit(ThemeSettings())
        val viewModel = SettingsViewModel(repository) { false }

        viewModel.themeSettings.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem()?.themeMode)
            viewModel.setThemeMode(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, awaitItem()?.themeMode)
            viewModel.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem()?.themeMode)
        }
    }

    @Test
    fun togglingDynamicColorUpdatesState() = runTest {
        repository.emit(ThemeSettings())
        val viewModel = SettingsViewModel(repository) { false }

        viewModel.themeSettings.test {
            assertEquals(true, awaitItem()?.dynamicColor)
            viewModel.setDynamicColor(false)
            assertEquals(false, awaitItem()?.dynamicColor)
        }
    }

    @Test
    fun reselectingCurrentModeDoesNotEmit() = runTest {
        repository.emit(ThemeSettings(ThemeMode.DARK))
        val viewModel = SettingsViewModel(repository) { false }

        viewModel.themeSettings.test {
            assertEquals(ThemeMode.DARK, awaitItem()?.themeMode)
            viewModel.setThemeMode(ThemeMode.DARK)
            expectNoEvents()
        }
    }
}
