package com.privacypal.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacypal.app.data.settings.SettingsRepository
import com.privacypal.app.domain.model.ThemeSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(settingsRepository: SettingsRepository) : ViewModel() {

    val uiState: StateFlow<MainUiState> = settingsRepository.themeSettings
        .map<ThemeSettings, MainUiState> { MainUiState.Ready(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = MainUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(val themeSettings: ThemeSettings) : MainUiState
}
