package com.privacypal.app.domain.model

/** Category an app declares in its manifest (`android:appCategory`). Most apps declare none. */
enum class AppCategory {
    UNDEFINED,
    GAME,
    AUDIO,
    VIDEO,
    IMAGE,
    SOCIAL,
    NEWS,
    MAPS,
    PRODUCTIVITY,
    ACCESSIBILITY,
}

/** A tracked permission an app requests, and whether it is currently granted. */
data class PermissionState(val name: String, val group: PermissionGroup, val granted: Boolean)

data class InstalledApp(
    val packageName: String,
    val label: String,
    val category: AppCategory,
    val isSystem: Boolean,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val permissions: List<PermissionState>,
) {
    /** Groups with at least one granted permission. */
    val grantedGroups: Set<PermissionGroup>
        get() = permissions.filter { it.granted }.mapTo(linkedSetOf()) { it.group }
}
