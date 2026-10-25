package com.privacypal.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.privacypal.app.domain.watchdog.Confidence
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorEventDao {

    @Query("SELECT * FROM sensor_events ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SensorEventEntity>>

    @Insert
    suspend fun insert(event: SensorEventEntity): Long

    @Query(
        "UPDATE sensor_events SET likelyResolved = 1, likelyPackage = :packageName, confidence = :confidence, " +
            "appInForeground = :inForeground WHERE id = :id",
    )
    suspend fun setLikelyApp(id: Long, packageName: String?, confidence: Confidence?, inForeground: Boolean?)

    @Query("UPDATE sensor_events SET endedAt = MAX(startedAt, :endedAt) WHERE id = :id")
    suspend fun setEnded(id: Long, endedAt: Long)

    @Query("DELETE FROM sensor_events WHERE id = :id")
    suspend fun delete(id: Long)

    /** Marks uses left open by an earlier run, whose end PrivacyPal never saw. */
    @Query("UPDATE sensor_events SET interrupted = 1 WHERE endedAt IS NULL")
    suspend fun markOpenInterrupted()

    @Query("DELETE FROM sensor_events WHERE startedAt < :cutoff")
    suspend fun deleteStartedBefore(cutoff: Long): Int
}

@Dao
interface AppRuleDao {

    @Query("SELECT * FROM app_rules")
    fun observeAll(): Flow<List<AppRuleEntity>>

    @Upsert
    suspend fun upsert(rule: AppRuleEntity)
}
