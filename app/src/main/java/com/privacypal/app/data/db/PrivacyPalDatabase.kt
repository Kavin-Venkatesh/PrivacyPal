package com.privacypal.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AppEntity::class, PermissionGrantEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PrivacyPalDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        const val NAME = "privacypal.db"
    }
}
