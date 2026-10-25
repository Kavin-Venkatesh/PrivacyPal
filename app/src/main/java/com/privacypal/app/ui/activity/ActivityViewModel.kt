package com.privacypal.app.ui.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.privacypal.app.data.apps.AppsRepository
import com.privacypal.app.data.watchdog.SensorUseRepository
import com.privacypal.app.data.watchdog.WatchdogSettingsRepository
import com.privacypal.app.domain.watchdog.ActivityFilter
import com.privacypal.app.domain.watchdog.ActivityTimeline
import com.privacypal.app.domain.watchdog.DayGroup
import com.privacypal.app.domain.watchdog.Sensor
import com.privacypal.app.ui.navigation.ActivityRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface ActivityUiState {
    data object Loading : ActivityUiState

    /** Nothing recorded (in the history period). */
    data class Empty(val watchdogOn: Boolean) : ActivityUiState

    /**
     * @property days uses matching [filter], grouped by day; empty when the filter matches nothing.
     * @property apps likely apps that appear in the history, for the app filter.
     * @property labels app names by package; a missing package shows its package name.
     * @property highlightId the use an alert was opened for, or null.
     */
    data class Content(
        val days: List<DayGroup>,
        val filter: ActivityFilter,
        val apps: List<String>,
        val labels: Map<String, String>,
        val highlightId: Long?,
    ) : ActivityUiState
}

/** What the Activity screen can do. */
data class ActivityActions(
    val onToggleSensor: (Sensor) -> Unit = {},
    val onToggleScreenOff: () -> Unit = {},
    val onSelectApp: (String?) -> Unit = {},
    val onClearFilters: () -> Unit = {},
    val onAppClick: (String) -> Unit = {},
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    sensorUses: SensorUseRepository,
    apps: AppsRepository,
    watchdogSettings: WatchdogSettingsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val highlightId: Long? =
        savedStateHandle.toRoute<ActivityRoute>().eventId.takeIf { it != ActivityRoute.NO_EVENT }

    // Kept in SavedStateHandle so filters survive process death.
    private val filter: StateFlow<ActivityFilter> = combine(
        savedStateHandle.getStateFlow(KEY_SENSORS, emptyList<String>()),
        savedStateHandle.getStateFlow(KEY_SCREEN_OFF, false),
        savedStateHandle.getStateFlow<String?>(KEY_PACKAGE, null),
    ) { sensors, screenOff, packageName ->
        ActivityFilter(sensors.mapTo(mutableSetOf()) { Sensor.valueOf(it) }, screenOff, packageName)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ActivityFilter())

    private val labels = apps.apps.map { list -> list.associate { it.packageName to it.label } }

    val uiState: StateFlow<ActivityUiState> = combine(
        sensorUses.uses,
        filter,
        labels,
        watchdogSettings.enabled,
    ) { uses, filter, labels, watchdogOn ->
        if (uses.isEmpty()) {
            ActivityUiState.Empty(watchdogOn)
        } else {
            ActivityUiState.Content(
                days = ActivityTimeline.groupByDay(ActivityTimeline.filter(uses, filter), ZoneId.systemDefault()),
                filter = filter,
                apps = ActivityTimeline.likelyPackages(uses),
                labels = labels,
                highlightId = highlightId,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ActivityUiState.Loading)

    fun toggleSensor(sensor: Sensor) {
        val current = filter.value.sensors
        val next = if (sensor in current) current - sensor else current + sensor
        savedStateHandle[KEY_SENSORS] = next.map { it.name }
    }

    fun toggleScreenOff() {
        savedStateHandle[KEY_SCREEN_OFF] = !filter.value.screenOffOnly
    }

    fun selectApp(packageName: String?) {
        savedStateHandle[KEY_PACKAGE] = packageName
    }

    fun clearFilters() {
        savedStateHandle[KEY_SENSORS] = emptyList<String>()
        savedStateHandle[KEY_SCREEN_OFF] = false
        savedStateHandle[KEY_PACKAGE] = null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val KEY_SENSORS = "filter_sensors"
        const val KEY_SCREEN_OFF = "filter_screen_off"
        const val KEY_PACKAGE = "filter_package"
    }
}
