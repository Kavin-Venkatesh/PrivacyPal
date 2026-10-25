package com.privacypal.app.domain.watchdog

/** What happened to a sensor's session after one [SensorEvent]. */
sealed interface SessionChange {
    val session: SensorSession

    data class Started(override val session: SensorSession) : SessionChange

    data class Ended(override val session: SensorSession) : SessionChange

    /** The session turned out not to be real use and should be forgotten. */
    data class Discarded(override val session: SensorSession) : SessionChange
}

/**
 * Pairs start/stop signals into sessions, one sensor at a time. A session starts when the first
 * source of a sensor goes into use and ends when the last one is released, so overlapping sources
 * produce one session. Duplicate starts and stops for an idle source are ignored. Not thread-safe:
 * feed it from one coroutine.
 */
class SensorSessionTracker {

    private val activeSources = mutableMapOf<Sensor, MutableSet<String>>()
    private val openSessions = mutableMapOf<Sensor, SensorSession>()

    fun onEvent(event: SensorEvent): SessionChange? {
        val sources = activeSources.getOrPut(event.sensor) { mutableSetOf() }
        return when (event.type) {
            SensorEvent.Type.STARTED -> start(event, sources)

            SensorEvent.Type.STOPPED -> release(event, sources)?.let {
                SessionChange.Ended(it.copy(endedAtMs = maxOf(event.timestampMs, it.startedAtMs)))
            }

            SensorEvent.Type.CANCELLED -> release(event, sources)?.let(SessionChange::Discarded)
        }
    }

    private fun start(event: SensorEvent, sources: MutableSet<String>): SessionChange? {
        val wasIdle = sources.isEmpty()
        val added = sources.add(event.sourceId)
        return if (added && wasIdle) {
            SensorSession(event.sensor, startedAtMs = event.timestampMs)
                .also { openSessions[event.sensor] = it }
                .let(SessionChange::Started)
        } else {
            null
        }
    }

    /** Releases a source; returns the open session if that was the sensor's last active source. */
    private fun release(event: SensorEvent, sources: MutableSet<String>): SensorSession? =
        if (sources.remove(event.sourceId) && sources.isEmpty()) openSessions.remove(event.sensor) else null

    /** Sessions still running, e.g. to close them when monitoring stops. */
    fun openSessions(): List<SensorSession> = openSessions.values.toList()
}
