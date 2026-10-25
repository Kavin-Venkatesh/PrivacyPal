package com.privacypal.app.data.scanner

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import com.privacypal.app.domain.model.AppCategory
import com.privacypal.app.domain.model.InstalledApp
import com.privacypal.app.domain.model.PermissionState
import com.privacypal.app.domain.permissions.PermissionGroups

/**
 * Converts a [PackageInfo] fetched with GET_PERMISSIONS. Only tracked permissions are kept; a
 * permission counts as granted when its REQUESTED_PERMISSION_GRANTED flag is set.
 *
 * Like the system permission manager, an app only counts as "system" (hidden by default) when it is
 * preinstalled and has no launcher icon.
 */
internal fun PackageInfo.toInstalledApp(label: String, hasLauncherIcon: Boolean): InstalledApp {
    val names = requestedPermissions.orEmpty()
    val flags = requestedPermissionsFlags
    val permissions = names.withIndex()
        .mapNotNull { (index, name) ->
            val group = PermissionGroups.groupOf(name) ?: return@mapNotNull null
            val granted = flags != null && index < flags.size &&
                flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
            PermissionState(name = name, group = group, granted = granted)
        }
        .distinctBy { it.name }
    val appInfo = applicationInfo
    val preinstalled = appInfo != null &&
        appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

    return InstalledApp(
        packageName = packageName,
        label = label,
        category = appInfo?.category.toAppCategory(),
        isSystem = preinstalled && !hasLauncherIcon,
        versionCode = PackageInfoCompat.getLongVersionCode(this),
        firstInstallTime = firstInstallTime,
        lastUpdateTime = lastUpdateTime,
        permissions = permissions,
    )
}

internal fun Int?.toAppCategory(): AppCategory = when (this) {
    ApplicationInfo.CATEGORY_GAME -> AppCategory.GAME
    ApplicationInfo.CATEGORY_AUDIO -> AppCategory.AUDIO
    ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
    ApplicationInfo.CATEGORY_IMAGE -> AppCategory.IMAGE
    ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
    ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
    ApplicationInfo.CATEGORY_MAPS -> AppCategory.MAPS
    ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
    ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.ACCESSIBILITY
    else -> AppCategory.UNDEFINED
}
