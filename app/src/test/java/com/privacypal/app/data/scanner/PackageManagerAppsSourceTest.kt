package com.privacypal.app.data.scanner

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PackageManagerAppsSourceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowPm get() = shadowOf(context.packageManager)
    private lateinit var source: PackageManagerAppsSource

    @Before
    fun setUp() {
        source = PackageManagerAppsSource(context, UnconfinedTestDispatcher())
        install("com.example.camera", "Camera App", granted = listOf("android.permission.CAMERA"), launcher = true)
        install("com.example.service", "Background Service", system = true, launcher = false)
        install("com.example.chrome", "Preinstalled Browser", system = true, launcher = true)
    }

    private fun install(
        pkg: String,
        label: String,
        granted: List<String> = emptyList(),
        system: Boolean = false,
        launcher: Boolean,
    ) {
        val info = PackageInfo().apply {
            packageName = pkg
            requestedPermissions = granted.toTypedArray()
            requestedPermissionsFlags = IntArray(granted.size) { PackageInfo.REQUESTED_PERMISSION_GRANTED }
            applicationInfo = ApplicationInfo().apply {
                packageName = pkg
                nonLocalizedLabel = label
                flags = if (system) ApplicationInfo.FLAG_SYSTEM else 0
            }
        }
        shadowPm.installPackage(info)
        if (launcher) {
            val component = ComponentName(pkg, "$pkg.Main")
            shadowPm.addActivityIfNotPresent(component)
            shadowPm.addIntentFilterForActivity(
                component,
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            )
        }
    }

    @Test
    fun scanAllMapsAppsAndExcludesPrivacyPal() = runTest {
        val apps = source.scanAll().associateBy { it.packageName }

        assertFalse(context.packageName in apps)
        assertEquals("Camera App", apps.getValue("com.example.camera").label)
        assertTrue(apps.getValue("com.example.camera").permissions.single().granted)
    }

    @Test
    fun systemFlagFollowsLauncherIcon() = runTest {
        val apps = source.scanAll().associateBy { it.packageName }

        assertTrue(apps.getValue("com.example.service").isSystem)
        assertFalse(apps.getValue("com.example.chrome").isSystem)
        assertFalse(apps.getValue("com.example.camera").isSystem)
    }

    @Test
    fun scanSinglePackage() = runTest {
        assertEquals("Camera App", source.scan("com.example.camera")?.label)
    }

    @Test
    fun scanMissingPackageReturnsNull() = runTest {
        assertNull(source.scan("com.example.not.installed"))
    }

    @Test
    fun scanSelfReturnsNull() = runTest {
        assertNull(source.scan(context.packageName))
    }
}
