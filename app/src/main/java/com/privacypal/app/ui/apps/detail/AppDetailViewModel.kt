package com.privacypal.app.ui.apps.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.privacypal.app.data.apps.AppsRepository
import com.privacypal.app.domain.apps.RatedApp
import com.privacypal.app.domain.apps.rated
import com.privacypal.app.ui.navigation.AppDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppDetailUiState {
    data object Loading : AppDetailUiState

    /** The app was uninstalled (or never scanned). */
    data object NotInstalled : AppDetailUiState

    data class Content(val rated: RatedApp) : AppDetailUiState
}

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val repository: AppsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val packageName: String = savedStateHandle.toRoute<AppDetailRoute>().packageName

    val uiState: StateFlow<AppDetailUiState> = repository.app(packageName)
        .map { app -> app?.let { AppDetailUiState.Content(it.rated()) } ?: AppDetailUiState.NotInstalled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppDetailUiState.Loading)

    /** Rescans this app, e.g. when the user comes back from system Settings. */
    fun refresh() {
        viewModelScope.launch { repository.refresh(packageName) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
