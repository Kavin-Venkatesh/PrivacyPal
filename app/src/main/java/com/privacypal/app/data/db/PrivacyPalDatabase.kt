package com.privacypal.app.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AppEntity::class, PermissionGrantEntity::class, SensorEventEntity::class, AppRuleEntity::class],
    version = 2,
    exportSchema = true,
    // v2 (Phase 2): adds sensor_events and app_rules; existing tables are unchanged.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class PrivacyPalDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    abstract fun sensorEventDao(): SensorEventDao

    abstract fun appRuleDao(): AppRuleDao

    companion object {
        const val NAME = "privacypal.db"
    }
}
