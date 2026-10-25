package com.privacypal.app.data.watchdog

import com.privacypal.app.data.db.SensorEventDao
import com.privacypal.app.data.db.SensorEventEntity
import com.privacypal.app.domain.watchdog.LikelyApp
import com.privacypal.app.domain.watchdog.SensorSession
import com.privacypal.app.domain.watchdog.SensorUse
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Stored microphone and camera use, newest first. Written by the watchdog service, read by the UI. */
interface SensorUseRepository {
    val uses: Flow<List<SensorUse>>

    suspend fun get(id: Long): SensorUse?

    /** Returns the id of the new record. */
    suspend fun recordStart(session: SensorSession, screenOn: Boolean): Long

    suspend fun recordLikelyApp(id: Long, likelyApp: LikelyApp)

    suspend fun recordEnd(id: Long, endedAtMs: Long)

    suspend fun discard(id: Long)

    /** Called when the watchdog starts: anything still open was cut off by an earlier stop. */
    suspend fun markOpenInterrupted()
}

class RoomSensorUseRepository @Inject constructor(private val dao: SensorEventDao) : SensorUseRepository {

    override val uses: Flow<List<SensorUse>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun get(id: Long): SensorUse? = dao.get(id)?.toDomain()

    override suspend fun recordStart(session: SensorSession, screenOn: Boolean): Long =
        dao.insert(SensorEventEntity(sensor = session.sensor, startedAt = session.startedAtMs, screenOn = screenOn))

    override suspend fun recordLikelyApp(id: Long, likelyApp: LikelyApp) =
        dao.setLikelyApp(id, likelyApp.packageName, likelyApp.confidence, likelyApp.inForeground)

    override suspend fun recordEnd(id: Long, endedAtMs: Long) = dao.setEnded(id, endedAtMs)

    override suspend fun discard(id: Long) = dao.delete(id)

    override suspend fun markOpenInterrupted() = dao.markOpenInterrupted()
}

private fun SensorEventEntity.toDomain() = SensorUse(
    id = id,
    sensor = sensor,
    startedAtMs = startedAt,
    endedAtMs = endedAt,
    interrupted = interrupted,
    screenOn = screenOn,
    likelyApp = if (likelyResolved) LikelyApp(likelyPackage, confidence, appInForeground) else null,
)
