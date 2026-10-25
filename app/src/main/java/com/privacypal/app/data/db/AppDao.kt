package com.privacypal.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Transaction
    @Query("SELECT * FROM apps ORDER BY label COLLATE NOCASE")
    fun observeAll(): Flow<List<AppWithGrants>>

    @Transaction
    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    fun observe(packageName: String): Flow<AppWithGrants?>

    @Query("SELECT packageName FROM apps")
    suspend fun packageNames(): List<String>

    @Upsert
    suspend fun upsertApps(apps: List<AppEntity>)

    @Insert
    suspend fun insertGrants(grants: List<PermissionGrantEntity>)

    @Query("DELETE FROM permission_grants")
    suspend fun deleteAllGrants()

    @Query("DELETE FROM permission_grants WHERE packageName = :packageName")
    suspend fun deleteGrants(packageName: String)

    /** Callers must keep [packageNames] under SQLite's variable limit (999 on Android 10). */
    @Query("DELETE FROM apps WHERE packageName IN (:packageNames)")
    suspend fun deleteApps(packageNames: List<String>)

    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)

    /** Makes the table match a full scan: upserts every app, replaces all grants, drops apps not in [apps]. */
    @Transaction
    suspend fun replaceAll(apps: List<AppEntity>, grants: List<PermissionGrantEntity>) {
        val keep = apps.mapTo(hashSetOf()) { it.packageName }
        packageNames().filterNot { it in keep }.chunked(DELETE_CHUNK).forEach { deleteApps(it) }
        upsertApps(apps)
        deleteAllGrants()
        grants.chunked(INSERT_CHUNK).forEach { insertGrants(it) }
    }

    @Transaction
    suspend fun replaceApp(app: AppEntity, grants: List<PermissionGrantEntity>) {
        upsertApps(listOf(app))
        deleteGrants(app.packageName)
        insertGrants(grants)
    }

    companion object {
        const val DELETE_CHUNK = 500

        // Four columns per grant row: stay well under 999 bound variables per statement.
        const val INSERT_CHUNK = 200
    }
}
