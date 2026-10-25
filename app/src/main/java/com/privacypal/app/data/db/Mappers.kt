package com.privacypal.app.data.db

import com.privacypal.app.domain.model.InstalledApp
import com.privacypal.app.domain.model.PermissionState

internal fun InstalledApp.toEntity(scannedAt: Long) = AppEntity(
    packageName = packageName,
    label = label,
    category = category,
    isSystem = isSystem,
    versionCode = versionCode,
    firstInstallTime = firstInstallTime,
    lastUpdateTime = lastUpdateTime,
    scannedAt = scannedAt,
)

internal fun InstalledApp.toGrantEntities() = permissions.map {
    PermissionGrantEntity(
        packageName = packageName,
        permission = it.name,
        permissionGroup = it.group,
        granted = it.granted,
    )
}

internal fun AppWithGrants.toDomain() = InstalledApp(
    packageName = app.packageName,
    label = app.label,
    category = app.category,
    isSystem = app.isSystem,
    versionCode = app.versionCode,
    firstInstallTime = app.firstInstallTime,
    lastUpdateTime = app.lastUpdateTime,
    permissions = grants
        .sortedBy { it.permission }
        .map { PermissionState(name = it.permission, group = it.permissionGroup, granted = it.granted) },
)
