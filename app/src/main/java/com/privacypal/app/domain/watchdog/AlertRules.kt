package com.privacypal.app.domain.watchdog

/** How loudly to tell the user about a [SensorUse]. */
enum class AlertLevel {
    NONE,
    NORMAL,

    /** Screen off, or no app on screen: the user can't have expected it. */
    URGENT,
}

object AlertRules {

    /**
     * The alert for [use], decided once its likely app is known. Blips under
     * [SensorSession.SHORT_SESSION_MS] that are already over, and apps in [mutedPackages], get none.
     * Use by an unknown app is always alerted.
     */
    fun level(use: SensorUse, mutedPackages: Set<String>): AlertLevel {
        val endedAt = use.endedAtMs
        val likely = use.likelyApp
        return when {
            endedAt != null && endedAt - use.startedAtMs < SensorSession.SHORT_SESSION_MS -> AlertLevel.NONE
            likely?.packageName != null && likely.packageName in mutedPackages -> AlertLevel.NONE
            !use.screenOn || likely?.inForeground == false -> AlertLevel.URGENT
            else -> AlertLevel.NORMAL
        }
    }
}
