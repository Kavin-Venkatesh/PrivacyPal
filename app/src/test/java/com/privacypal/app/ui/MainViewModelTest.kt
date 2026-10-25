package com.privacypal.app.ui

import app.cash.turbine.test
import com.privacypal.app.domain.model.ThemeMode
import com.privacypal.app.domain.model.ThemeSettings
import com.privacypal.app.testing.FakeSettingsRepository
import com.privacypal.app.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository()

    @Test
    fun startsLoadingUntilSettingsArrive() = runTest {
        val viewModel = MainViewModel(repository)

        viewModel.uiState.test {
            assertEquals(MainUiState.Loading, awaitItem())
            repository.emit(ThemeSettings())
            assertEquals(MainUiState.Ready(ThemeSettings()), awaitItem())
        }
    }

    @Test
    fun settingChangesArriveImmediately() = runTest {
        repository.emit(ThemeSettings())
        val viewModel = MainViewModel(repository)

        viewModel.uiState.test {
            assertEquals(MainUiState.Ready(ThemeSettings()), awaitItem())
            repository.setThemeMode(ThemeMode.DARK)
            assertEquals(MainUiState.Ready(ThemeSettings(ThemeMode.DARK)), awaitItem())
            repository.setDynamicColor(false)
            assertEquals(MainUiState.Ready(ThemeSettings(ThemeMode.DARK, dynamicColor = false)), awaitItem())
        }
    }
}
