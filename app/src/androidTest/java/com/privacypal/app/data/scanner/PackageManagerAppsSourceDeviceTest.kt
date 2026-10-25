package com.privacypal.app.data.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real scanner against the device's PackageManager (needs QUERY_ALL_PACKAGES visibility). */
@RunWith(AndroidJUnit4::class)
class PackageManagerAppsSourceDeviceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val source = PackageManagerAppsSource(context, Dispatchers.IO)

    @Test
    fun seesInstalledAppsButNotItself() = runBlocking {
        val apps = source.scanAll()

        assertTrue("expected many visible packages, got ${apps.size}", apps.size > MIN_EXPECTED_PACKAGES)
        assertFalse(apps.any { it.packageName == context.packageName })
        assertTrue(
            "some app should hold a granted tracked permission",
            apps.any { app ->
                app.permissions.any { it.granted }
            },
        )
    }

    @Test
    fun settingsAppIsVisibleWithLauncherIcon() = runBlocking {
        val settings = source.scan("com.android.settings")

        assertNotNull(settings)
        assertFalse("Settings has a launcher icon, so it is not hidden as system", settings!!.isSystem)
        assertNull(source.scan(context.packageName))
    }

    private companion object {
        // Without package visibility an app sees only a handful of packages.
        const val MIN_EXPECTED_PACKAGES = 30
    }
}
