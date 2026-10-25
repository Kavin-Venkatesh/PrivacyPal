package com.privacypal.app.domain.watchdog

enum class Sensor {
    MICROPHONE,
    CAMERA,
}

/**
 * A raw signal from a sensor monitor. [sourceId] tells sources of one sensor apart (a camera id; the
 * microphone has a single source).
 */
data class SensorEvent(val sensor: Sensor, val type: Type, val sourceId: String, val timestampMs: Long) {
    enum class Type {
        /** The source went into use. */
        STARTED,

        /** The source was released. */
        STOPPED,

        /** The last start was not real use (e.g. the flashlight took the camera); drop that session. */
        CANCELLED,
    }
}

/**
 * One continuous period in which a sensor was in use. Overlapping sources of the same sensor (two
 * cameras) merge into one session. [endedAtMs] is null while it is still running.
 */
data class SensorSession(val sensor: Sensor, val startedAtMs: Long, val endedAtMs: Long? = null) {
    val durationMs: Long?
        get() = endedAtMs?.let { it - startedAtMs }

    /** Under a second: kept, but shown as a brief access. */
    val isShort: Boolean
        get() = durationMs?.let { it < SHORT_SESSION_MS } ?: false

    companion object {
        const val SHORT_SESSION_MS = 1_000L
    }
}
