package com.privacypal.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacypal.app.data.apps.Clock
import com.privacypal.app.data.watchdog.SensorUseRepository
import com.privacypal.app.data.watchdog.WatchdogSettingsRepository
import com.privacypal.app.domain.watchdog.HistoryRetention
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The mic and camera history retention setting. */
@HiltViewModel
class HistorySettingsViewModel @Inject constructor(
    private val settings: WatchdogSettingsRepository,
    private val sensorUses: SensorUseRepository,
    private val clock: Clock,
) : ViewModel() {

    /** Null until loaded. */
    val retentionDays: StateFlow<Int?> =
        settings.retentionDays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** Saves the setting and drops older history right away instead of at the next daily cleanup. */
    fun setRetentionDays(days: Int) {
        viewModelScope.launch {
            settings.setRetentionDays(days)
            sensorUses.deleteStartedBefore(HistoryRetention.cutoff(clock.now(), days))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
