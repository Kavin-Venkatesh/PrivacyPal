package com.privacypal.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val file: File =
        ApplicationProvider.getApplicationContext<android.content.Context>().getDatabasePath("migration-test.db")

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = file,
        driver = AndroidSQLiteDriver(),
        databaseClass = PrivacyPalDatabase::class,
    )

    @Test
    fun migrate1To2KeepsAppsAndAddsWatchdogTables() {
        helper.createDatabase(1).apply {
            execSQL(
                "INSERT INTO apps (packageName, label, category, isSystem, versionCode, firstInstallTime, " +
                    "lastUpdateTime, scannedAt) VALUES ('com.example', 'Example', 'SOCIAL', 0, 1, 10, 20, 30)",
            )
            execSQL(
                "INSERT INTO permission_grants (packageName, permission, permissionGroup, granted) " +
                    "VALUES ('com.example', 'android.permission.CAMERA', 'CAMERA', 1)",
            )
            close()
        }

        // Validates the migrated schema against 2.json.
        helper.runMigrationsAndValidate(2).apply {
            assertEquals("Example", text("SELECT label FROM apps"))
            assertEquals("1", text("SELECT COUNT(*) FROM permission_grants"))
            execSQL(
                "INSERT INTO sensor_events (sensor, startedAt, interrupted, screenOn, likelyResolved) " +
                    "VALUES ('CAMERA', 100, 0, 1, 0)",
            )
            execSQL("INSERT INTO app_rules (packageName, muteAlerts) VALUES ('com.example', 1)")
            assertEquals("1", text("SELECT COUNT(*) FROM sensor_events"))
            close()
        }
    }

    private fun SQLiteConnection.text(sql: String): String = prepare(sql).use {
        it.step()
        it.getText(0)
    }
}
