package com.privacypal.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacypal.app.data.settings.SettingsRepository
import com.privacypal.app.data.watchdog.UsageAccessChecker
import com.privacypal.app.domain.model.ThemeMode
import com.privacypal.app.domain.model.ThemeSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val usageAccess: UsageAccessChecker,
) : ViewModel() {

    private val _usageAccessGranted = MutableStateFlow(usageAccess.isGranted())
    val usageAccessGranted: StateFlow<Boolean> = _usageAccessGranted.asStateFlow()

    /** Usage access changes in system Settings; call when the screen resumes. */
    fun refreshUsageAccess() {
        _usageAccessGranted.value = usageAccess.isGranted()
    }

    /** Null until the stored settings have been read. */
    val themeSettings: StateFlow<ThemeSettings?> = settingsRepository.themeSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = null,
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
