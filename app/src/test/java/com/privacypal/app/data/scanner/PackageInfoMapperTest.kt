package com.privacypal.app.data.scanner

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.privacypal.app.domain.model.AppCategory
import com.privacypal.app.domain.model.PermissionGroup
import com.privacypal.app.domain.model.PermissionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PackageInfoMapperTest {

    private fun packageInfo(
        permissions: Array<String>? = null,
        flags: IntArray? = null,
        appFlags: Int = 0,
        category: Int = ApplicationInfo.CATEGORY_UNDEFINED,
    ) = PackageInfo().apply {
        packageName = "com.example.app"
        requestedPermissions = permissions
        requestedPermissionsFlags = flags
        firstInstallTime = 100L
        lastUpdateTime = 200L
        longVersionCode = 42L
        applicationInfo = ApplicationInfo().apply {
            packageName = "com.example.app"
            this.flags = appFlags
            this.category = category
        }
    }

    @Test
    fun keepsOnlyTrackedPermissionsWithGrantState() {
        val info = packageInfo(
            permissions = arrayOf(
                "android.permission.CAMERA",
                "android.permission.INTERNET",
                "android.permission.RECORD_AUDIO",
            ),
            flags = intArrayOf(PackageInfo.REQUESTED_PERMISSION_GRANTED, PackageInfo.REQUESTED_PERMISSION_GRANTED, 0),
        )

        val app = info.toInstalledApp(label = "Example", hasLauncherIcon = true)

        assertEquals(
            listOf(
                PermissionState("android.permission.CAMERA", PermissionGroup.CAMERA, granted = true),
                PermissionState("android.permission.RECORD_AUDIO", PermissionGroup.MICROPHONE, granted = false),
            ),
            app.permissions,
        )
        assertEquals(setOf(PermissionGroup.CAMERA), app.grantedGroups)
    }

    @Test
    fun copiesMetadata() {
        val app = packageInfo(category = ApplicationInfo.CATEGORY_GAME).toInstalledApp("Example", true)

        assertEquals("com.example.app", app.packageName)
        assertEquals("Example", app.label)
        assertEquals(AppCategory.GAME, app.category)
        assertEquals(42L, app.versionCode)
        assertEquals(100L, app.firstInstallTime)
        assertEquals(200L, app.lastUpdateTime)
    }

    @Test
    fun noRequestedPermissionsGivesEmptyList() {
        val app = packageInfo(permissions = null, flags = null).toInstalledApp("Example", true)
        assertTrue(app.permissions.isEmpty())
        assertTrue(app.grantedGroups.isEmpty())
    }

    @Test
    fun missingFlagsMeansNotGranted() {
        val app = packageInfo(permissions = arrayOf("android.permission.CAMERA"), flags = null)
            .toInstalledApp("Example", true)
        assertFalse(app.permissions.single().granted)
    }

    @Test
    fun duplicatePermissionsAreCollapsed() {
        val app = packageInfo(
            permissions = arrayOf("android.permission.CAMERA", "android.permission.CAMERA"),
            flags = intArrayOf(PackageInfo.REQUESTED_PERMISSION_GRANTED, PackageInfo.REQUESTED_PERMISSION_GRANTED),
        ).toInstalledApp("Example", true)
        assertEquals(1, app.permissions.size)
    }

    @Test
    fun preinstalledWithoutLauncherIconIsSystem() {
        val app = packageInfo(appFlags = ApplicationInfo.FLAG_SYSTEM).toInstalledApp("Example", hasLauncherIcon = false)
        assertTrue(app.isSystem)
    }

    @Test
    fun preinstalledWithLauncherIconIsNotSystem() {
        val updatedSystemApp = packageInfo(
            appFlags =
                ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP,
        )
        assertFalse(updatedSystemApp.toInstalledApp("Example", hasLauncherIcon = true).isSystem)
    }

    @Test
    fun userAppWithoutLauncherIconIsNotSystem() {
        assertFalse(packageInfo().toInstalledApp("Example", hasLauncherIcon = false).isSystem)
    }

    @Test
    fun unknownCategoryValuesMapToUndefined() {
        assertEquals(AppCategory.UNDEFINED, ApplicationInfo.CATEGORY_UNDEFINED.toAppCategory())
        assertEquals(AppCategory.UNDEFINED, 999.toAppCategory())
        assertEquals(AppCategory.UNDEFINED, (null as Int?).toAppCategory())
        assertEquals(AppCategory.PRODUCTIVITY, ApplicationInfo.CATEGORY_PRODUCTIVITY.toAppCategory())
    }
}
