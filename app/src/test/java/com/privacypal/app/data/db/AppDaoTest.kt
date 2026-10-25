package com.privacypal.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.privacypal.app.domain.model.AppCategory
import com.privacypal.app.domain.model.PermissionGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDaoTest {

    private lateinit var db: PrivacyPalDatabase
    private lateinit var dao: AppDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PrivacyPalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.appDao()
    }

    @After
    fun tearDown() = db.close()

    private fun app(pkg: String, label: String = pkg) = AppEntity(
        packageName = pkg,
        label = label,
        category = AppCategory.SOCIAL,
        isSystem = false,
        versionCode = 1,
        firstInstallTime = 10,
        lastUpdateTime = 20,
        scannedAt = 30,
    )

    private fun grant(pkg: String, permission: String = "android.permission.CAMERA", granted: Boolean = true) =
        PermissionGrantEntity(pkg, permission, PermissionGroup.CAMERA, granted)

    @Test
    fun replaceAllStoresAppsWithGrantsSortedByLabel() = runTest {
        dao.replaceAll(listOf(app("b", "beta"), app("a", "Alpha")), listOf(grant("a"), grant("b", granted = false)))

        val rows = dao.observeAll().first()

        assertEquals(listOf("a", "b"), rows.map { it.app.packageName })
        assertEquals(listOf(true), rows[0].grants.map { it.granted })
        assertEquals(AppCategory.SOCIAL, rows[0].app.category)
        assertEquals(PermissionGroup.CAMERA, rows[0].grants.single().permissionGroup)
    }

    @Test
    fun replaceAllRemovesUninstalledAppsAndTheirGrants() = runTest {
        dao.replaceAll(listOf(app("a"), app("b")), listOf(grant("a"), grant("b")))

        dao.replaceAll(listOf(app("a")), listOf(grant("a")))

        assertEquals(listOf("a"), dao.packageNames())
        assertNull(dao.observe("b").first())
    }

    @Test
    fun replaceAllReplacesRevokedGrants() = runTest {
        dao.replaceAll(listOf(app("a")), listOf(grant("a", granted = true)))

        dao.replaceAll(listOf(app("a")), listOf(grant("a", granted = false)))

        assertEquals(false, dao.observe("a").first()!!.grants.single().granted)
    }

    @Test
    fun replaceAllHandlesMoreRowsThanSqliteVariableLimit() = runTest {
        val apps = (1..1200).map { app("pkg$it") }
        val grants = apps.flatMap {
            listOf(grant(it.packageName), grant(it.packageName, "android.permission.RECORD_AUDIO"))
        }

        dao.replaceAll(apps, grants)
        dao.replaceAll(apps.take(10), grants.take(20))

        assertEquals(10, dao.packageNames().size)
        assertEquals(20, dao.observeAll().first().sumOf { it.grants.size })
    }

    @Test
    fun deletingAppCascadesToGrants() = runTest {
        dao.replaceAll(listOf(app("a")), listOf(grant("a")))

        dao.deleteApp("a")

        assertTrue(dao.observeAll().first().isEmpty())
        dao.replaceApp(app("a"), emptyList())
        assertTrue(dao.observe("a").first()!!.grants.isEmpty())
    }

    @Test
    fun replaceAppUpdatesOneAppOnly() = runTest {
        dao.replaceAll(listOf(app("a"), app("b")), listOf(grant("a"), grant("b")))

        dao.replaceApp(app("a", "Renamed"), listOf(grant("a", granted = false)))

        val a = dao.observe("a").first()!!
        assertEquals("Renamed", a.app.label)
        assertEquals(false, a.grants.single().granted)
        assertEquals(true, dao.observe("b").first()!!.grants.single().granted)
    }

    @Test
    fun observeAllEmitsOnChange() = runTest {
        dao.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            dao.replaceAll(listOf(app("a")), emptyList())
            assertEquals(listOf("a"), awaitItem().map { it.app.packageName })
            dao.deleteApp("a")
            assertTrue(awaitItem().isEmpty())
        }
    }
}
