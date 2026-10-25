package com.privacypal.app.data.apps

import com.privacypal.app.domain.model.InstalledApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class RefreshState {
    /** No scan has finished yet in this process. */
    NOT_STARTED,
    RUNNING,
    IDLE,
    FAILED,
}

interface AppsRepository {
    /** Stored apps, sorted by label. Empty until the first scan has been saved. */
    val apps: Flow<List<InstalledApp>>

    val refreshState: StateFlow<RefreshState>

    fun app(packageName: String): Flow<InstalledApp?>

    /** Rescans every app; removes apps that are no longer installed. */
    suspend fun refresh()

    /** Rescans one app; removes it if it is no longer installed. */
    suspend fun refresh(packageName: String)
}
