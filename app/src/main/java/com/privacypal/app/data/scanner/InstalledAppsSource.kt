package com.privacypal.app.data.scanner

import com.privacypal.app.domain.model.InstalledApp

/** Reads installed apps and their tracked permissions from the system. PrivacyPal itself is never included. */
interface InstalledAppsSource {
    suspend fun scanAll(): List<InstalledApp>

    /** The app, or null if it is not installed (or is PrivacyPal). */
    suspend fun scan(packageName: String): InstalledApp?
}
