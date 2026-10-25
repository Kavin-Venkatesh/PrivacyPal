package com.privacypal.app.testing

import com.privacypal.app.data.apps.AppsRepository
import com.privacypal.app.data.apps.RefreshState
import com.privacypal.app.domain.model.AppCategory
import com.privacypal.app.domain.model.InstalledApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory AppsRepository; tests drive [appsFlow] and [refreshState] directly. */
class FakeAppsRepository(apps: List<InstalledApp> = emptyList(), refreshState: RefreshState = RefreshState.IDLE) :
    AppsRepository {
    val appsFlow = MutableStateFlow(apps)
    override val refreshState = MutableStateFlow(refreshState)
    var refreshCount = 0
        private set
    val refreshedPackages = mutableListOf<String>()

    override val apps: Flow<List<InstalledApp>> = appsFlow

    override fun app(packageName: String): Flow<InstalledApp?> =
        appsFlow.map { list -> list.firstOrNull { it.packageName == packageName } }

    override suspend fun refresh() {
        refreshCount++
    }

    override suspend fun refresh(packageName: String) {
        refreshedPackages += packageName
    }
}

/** A realistic mix for screenshots and UI tests: one app per risk level plus a system app. */
val sampleApps: List<InstalledApp> = listOf(
    testApp(
        packageName = "com.example.flashlight",
        label = "Bright Torch",
        category = AppCategory.GAME,
        granted = listOf(RECORD_AUDIO, FINE_LOCATION, READ_CONTACTS, CAMERA),
        firstInstallTime = 5_000L,
    ),
    testApp(
        packageName = "com.example.chat",
        label = "Chatter",
        category = AppCategory.SOCIAL,
        granted = listOf(CAMERA, RECORD_AUDIO, READ_CONTACTS, POST_NOTIFICATIONS),
        firstInstallTime = 4_000L,
    ),
    testApp(
        packageName = "com.example.weather",
        label = "Weather Now",
        granted = listOf(FINE_LOCATION, BACKGROUND_LOCATION, POST_NOTIFICATIONS),
        firstInstallTime = 3_000L,
    ),
    testApp(
        packageName = "com.example.notes",
        label = "Notes",
        category = AppCategory.PRODUCTIVITY,
        granted = listOf(POST_NOTIFICATIONS),
        requestedOnly = listOf(RECORD_AUDIO),
        firstInstallTime = 2_000L,
    ),
    testApp(
        packageName = "com.example.calculator",
        label = "Calculator",
        firstInstallTime = 1_000L,
    ),
    testApp(
        packageName = "com.android.systemthing",
        label = "System Thing",
        isSystem = true,
        granted = listOf(READ_SMS),
    ),
)
