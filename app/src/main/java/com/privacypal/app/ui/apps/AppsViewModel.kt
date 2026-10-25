package com.privacypal.app.ui.apps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacypal.app.data.apps.AppsRepository
import com.privacypal.app.data.apps.RefreshState
import com.privacypal.app.domain.apps.AppFilter
import com.privacypal.app.domain.apps.AppQuery
import com.privacypal.app.domain.apps.AppSort
import com.privacypal.app.domain.apps.AppsSummary
import com.privacypal.app.domain.apps.RatedApp
import com.privacypal.app.domain.apps.applyQuery
import com.privacypal.app.domain.apps.rated
import com.privacypal.app.domain.apps.summarize
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppsUiState {
    data object Loading : AppsUiState

    /** The first scan failed and there is nothing stored to show. */
    data object Error : AppsUiState

    /**
     * @property rows apps after search, filter and sort.
     * @property refreshFailed the latest rescan failed; [rows] show the previous scan.
     */
    data class Content(val summary: AppsSummary, val rows: List<RatedApp>, val refreshFailed: Boolean) : AppsUiState
}

/** Apps list state. The query lives in [SavedStateHandle], so it survives rotation and process death. */
@HiltViewModel
class AppsViewModel @Inject constructor(
    private val repository: AppsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val query: StateFlow<AppQuery> = combine(
        savedStateHandle.getStateFlow(KEY_SEARCH, ""),
        savedStateHandle.getStateFlow(KEY_FILTER, AppFilter.ALL.name),
        savedStateHandle.getStateFlow(KEY_SORT, AppSort.RISK.name),
        savedStateHandle.getStateFlow(KEY_SHOW_SYSTEM, false),
    ) { search, filter, sort, showSystem ->
        AppQuery(
            search = search,
            filter = enumValueOrDefault(filter, AppFilter.ALL),
            sort = enumValueOrDefault(sort, AppSort.RISK),
            showSystem = showSystem,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, currentQuery())

    private val ratedApps = repository.apps.map { apps -> apps.map { it.rated() } }

    val uiState: StateFlow<AppsUiState> = combine(ratedApps, repository.refreshState, query) { apps, refresh, query ->
        when {
            apps.isNotEmpty() || refresh == RefreshState.IDLE -> AppsUiState.Content(
                summary = summarize(apps, query.showSystem),
                rows = apps.applyQuery(query),
                refreshFailed = refresh == RefreshState.FAILED,
            )

            refresh == RefreshState.FAILED -> AppsUiState.Error

            else -> AppsUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppsUiState.Loading)

    /**
     * The search box text. Compose state, updated synchronously, so fast typing never loses characters;
     * the (asynchronous) [query] follows it.
     */
    var searchText by mutableStateOf(savedStateHandle.get<String>(KEY_SEARCH) ?: "")
        private set

    fun onSearchChange(search: String) {
        searchText = search
        savedStateHandle[KEY_SEARCH] = search
    }

    fun onFilterChange(filter: AppFilter) {
        savedStateHandle[KEY_FILTER] = filter.name
    }

    fun onSortChange(sort: AppSort) {
        savedStateHandle[KEY_SORT] = sort.name
    }

    fun onShowSystemChange(show: Boolean) {
        savedStateHandle[KEY_SHOW_SYSTEM] = show
    }

    fun clearFilters() {
        onSearchChange("")
        savedStateHandle[KEY_FILTER] = AppFilter.ALL.name
    }

    fun retry() {
        viewModelScope.launch { repository.refresh() }
    }

    private fun currentQuery() = AppQuery(
        search = savedStateHandle[KEY_SEARCH] ?: "",
        filter = enumValueOrDefault(savedStateHandle[KEY_FILTER], AppFilter.ALL),
        sort = enumValueOrDefault(savedStateHandle[KEY_SORT], AppSort.RISK),
        showSystem = savedStateHandle[KEY_SHOW_SYSTEM] ?: false,
    )

    private companion object {
        const val KEY_SEARCH = "search"
        const val KEY_FILTER = "filter"
        const val KEY_SORT = "sort"
        const val KEY_SHOW_SYSTEM = "showSystem"
        const val STOP_TIMEOUT_MS = 5_000L

        inline fun <reified T : Enum<T>> enumValueOrDefault(name: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}
