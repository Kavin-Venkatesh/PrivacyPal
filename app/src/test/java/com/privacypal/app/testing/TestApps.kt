package com.privacypal.app.testing

import com.privacypal.app.data.scanner.InstalledAppsSource
import com.privacypal.app.domain.model.AppCategory
import com.privacypal.app.domain.model.InstalledApp
import com.privacypal.app.domain.model.PermissionState
import com.privacypal.app.domain.permissions.PermissionGroups

/** Builds an app; [granted] and [requestedOnly] take raw permission names. */
fun testApp(
    packageName: String = "com.example.app",
    label: String = "Example",
    category: AppCategory = AppCategory.UNDEFINED,
    isSystem: Boolean = false,
    granted: List<String> = emptyList(),
    requestedOnly: List<String> = emptyList(),
    firstInstallTime: Long = 1_000L,
    lastUpdateTime: Long = firstInstallTime,
    versionCode: Long = 1L,
): InstalledApp = InstalledApp(
    packageName = packageName,
    label = label,
    category = category,
    isSystem = isSystem,
    versionCode = versionCode,
    firstInstallTime = firstInstallTime,
    lastUpdateTime = lastUpdateTime,
    permissions = granted.map { PermissionState(it, requireNotNull(PermissionGroups.groupOf(it)), granted = true) } +
        requestedOnly.map { PermissionState(it, requireNotNull(PermissionGroups.groupOf(it)), granted = false) },
)

const val CAMERA = "android.permission.CAMERA"
const val RECORD_AUDIO = "android.permission.RECORD_AUDIO"
const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
const val BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"
const val READ_CONTACTS = "android.permission.READ_CONTACTS"
const val READ_SMS = "android.permission.READ_SMS"
const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

/** In-memory InstalledAppsSource; set [apps] or [failure] before scanning. */
class FakeInstalledAppsSource(var apps: List<InstalledApp> = emptyList()) : InstalledAppsSource {
    var failure: Exception? = null
    var scanAllCount = 0
        private set

    override suspend fun scanAll(): List<InstalledApp> {
        scanAllCount++
        failure?.let { throw it }
        return apps
    }

    override suspend fun scan(packageName: String): InstalledApp? {
        failure?.let { throw it }
        return apps.firstOrNull { it.packageName == packageName }
    }
}
