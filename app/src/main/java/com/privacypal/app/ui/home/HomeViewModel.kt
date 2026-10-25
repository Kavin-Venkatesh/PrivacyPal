package com.privacypal.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacypal.app.data.apps.AppsRepository
import com.privacypal.app.data.apps.RefreshState
import com.privacypal.app.data.watchdog.UsageAccessChecker
import com.privacypal.app.domain.apps.AppQuery
import com.privacypal.app.domain.apps.RatedApp
import com.privacypal.app.domain.apps.applyQuery
import com.privacypal.app.domain.apps.rated
import com.privacypal.app.domain.risk.RiskLevel
import com.privacypal.app.service.WatchdogController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    /** The first scan is still running. */
    data object Loading : HomeUiState

    /** Nothing has been checked yet (or the first scan failed). */
    data object Empty : HomeUiState

    /**
     * Permission summary over the apps the user sees (system apps hidden).
     *
     * @property riskiest up to [TOP_COUNT] apps rated MEDIUM or HIGH, worst first.
     */
    data class Content(val appsChecked: Int, val highRisk: Int, val riskiest: List<RatedApp>) : HomeUiState

    companion object {
        const val TOP_COUNT = 3
    }
}

/** The mic and camera watchdog card on Home. */
data class WatchdogCardState(
    val enabled: Boolean,
    val usageAccessGranted: Boolean,
    val notificationsAllowed: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: AppsRepository,
    private val watchdogController: WatchdogController,
    private val usageAccess: UsageAccessChecker,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(repository.apps, repository.refreshState) { apps, refresh ->
        when {
            apps.isNotEmpty() -> {
                val visible = apps.map { it.rated() }.applyQuery(AppQuery())
                HomeUiState.Content(
                    appsChecked = visible.size,
                    highRisk = visible.count { it.risk.level == RiskLevel.HIGH },
                    riskiest = visible.filter { it.risk.level >= RiskLevel.MEDIUM }.take(HomeUiState.TOP_COUNT),
                )
            }

            refresh == RefreshState.NOT_STARTED || refresh == RefreshState.RUNNING -> HomeUiState.Loading

            else -> HomeUiState.Empty
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState.Loading)

    private val usageAccessGranted = MutableStateFlow(usageAccess.isGranted())

    /** null until the setting has loaded. */
    val watchdog: StateFlow<WatchdogCardState?> =
        combine(watchdogController.enabled, usageAccessGranted) { enabled, granted ->
            WatchdogCardState(enabled = enabled, usageAccessGranted = granted)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** Usage access is changed in system Settings; check again when Home comes back. */
    fun refreshUsageAccess() {
        usageAccessGranted.value = usageAccess.isGranted()
    }

    fun setWatchdogEnabled(enabled: Boolean) {
        viewModelScope.launch { watchdogController.setEnabled(enabled) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** What the watchdog card on Home can do. */
data class WatchdogActions(
    val onEnabledChange: (Boolean) -> Unit = {},
    val onAllowNotifications: () -> Unit = {},
    val onSetUpUsageAccess: () -> Unit = {},
)
