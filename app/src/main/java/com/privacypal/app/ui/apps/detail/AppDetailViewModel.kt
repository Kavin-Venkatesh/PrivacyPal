package com.privacypal.app.ui.apps.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.privacypal.app.data.apps.AppsRepository
import com.privacypal.app.data.watchdog.AppRuleRepository
import com.privacypal.app.data.watchdog.SensorUseRepository
import com.privacypal.app.domain.apps.RatedApp
import com.privacypal.app.domain.apps.rated
import com.privacypal.app.domain.watchdog.SensorUse
import com.privacypal.app.ui.navigation.AppDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppDetailUiState {
    data object Loading : AppDetailUiState

    /** The app was uninstalled (or never scanned). */
    data object NotInstalled : AppDetailUiState

    data class Content(val rated: RatedApp) : AppDetailUiState
}

/**
 * Mic and camera use where this app was the likely app.
 *
 * @property recent the newest [AppDetailViewModel.RECENT_COUNT] uses.
 */
data class AppSensorHistory(val recent: List<SensorUse>, val alertsOn: Boolean)

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val repository: AppsRepository,
    sensorUses: SensorUseRepository,
    private val appRules: AppRuleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val packageName: String = savedStateHandle.toRoute<AppDetailRoute>().packageName

    val sensorHistory: StateFlow<AppSensorHistory?> =
        combine(sensorUses.uses, appRules.mutedPackages) { uses, muted ->
            AppSensorHistory(
                recent = uses.filter { it.likelyApp?.packageName == packageName }.take(RECENT_COUNT),
                alertsOn = packageName !in muted,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun setAlertsOn(on: Boolean) {
        viewModelScope.launch { appRules.setMuted(packageName, muted = !on) }
    }

    val uiState: StateFlow<AppDetailUiState> = repository.app(packageName)
        .map { app -> app?.let { AppDetailUiState.Content(it.rated()) } ?: AppDetailUiState.NotInstalled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppDetailUiState.Loading)

    /** Rescans this app, e.g. when the user comes back from system Settings. */
    fun refresh() {
        viewModelScope.launch { repository.refresh(packageName) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        const val RECENT_COUNT = 10
    }
}
