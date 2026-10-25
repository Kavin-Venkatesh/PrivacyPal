package com.privacypal.app.domain.watchdog

/**
 * A stored period of microphone or camera use.
 *
 * @property endedAtMs null while in use, or when [interrupted].
 * @property likelyApp null until it has been looked up (a few seconds after the start).
 */
data class SensorUse(
    val id: Long,
    val sensor: Sensor,
    val startedAtMs: Long,
    val endedAtMs: Long?,
    val interrupted: Boolean,
    val screenOn: Boolean,
    val likelyApp: LikelyApp?,
)
