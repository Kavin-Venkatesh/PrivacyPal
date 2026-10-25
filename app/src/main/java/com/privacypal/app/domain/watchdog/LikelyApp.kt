package com.privacypal.app.domain.watchdog

/** How sure PrivacyPal is that [LikelyApp.packageName] is the app using the sensor. Never certain. */
enum class Confidence {
    /** The app came to the screen within [LikelyAppRules.HIGH_WINDOW_MS] of the sensor starting. */
    HIGH,

    /** The app was on screen and had been for up to [LikelyAppRules.MEDIUM_WINDOW_MS]. */
    MEDIUM,

    /** The app had been on screen for longer; something else may be using the sensor. */
    LOW,
}

/** An app coming to or leaving the screen, as reported by usage stats. */
data class UsageEvent(val packageName: String, val type: Type, val timeMs: Long) {
    enum class Type {
        RESUMED,
        PAUSED,
    }
}

/**
 * Best guess at the app using a sensor. Android does not say which app it is, so this is the app that
 * was on screen when the sensor started.
 *
 * @property packageName null when unknown: no usage access, or nothing but the home screen (or
 * PrivacyPal) was on screen, which means the sensor was used by an app in the background.
 * @property inForeground whether an app (the likely one) was on screen; null without usage access.
 */
data class LikelyApp(val packageName: String?, val confidence: Confidence?, val inForeground: Boolean?) {
    companion object {
        /** Usage access is off: nothing is known. */
        val Unavailable = LikelyApp(packageName = null, confidence = null, inForeground = null)

        /** Usage access is on, but no app was on screen. */
        val NothingOnScreen = LikelyApp(packageName = null, confidence = null, inForeground = false)
    }
}

object LikelyAppRules {
    const val HIGH_WINDOW_MS = 2_000L
    const val MEDIUM_WINDOW_MS = 60_000L

    /**
     * The app on screen at [atMs] according to [events]. Events up to [HIGH_WINDOW_MS] after [atMs]
     * count too, because an app often starts the camera a moment before its screen is reported.
     * [ignored] packages (home screens, system UI, PrivacyPal) are never the likely app.
     */
    fun resolve(events: List<UsageEvent>, atMs: Long, ignored: Set<String>): LikelyApp {
        var current: String? = null
        var since = 0L
        events.asSequence()
            .filter { it.timeMs <= atMs + HIGH_WINDOW_MS }
            .sortedBy { it.timeMs }
            .forEach { event ->
                when (event.type) {
                    UsageEvent.Type.RESUMED -> {
                        current = event.packageName
                        since = event.timeMs
                    }

                    UsageEvent.Type.PAUSED -> if (event.packageName == current) current = null
                }
            }
        val app = current?.takeUnless { it in ignored } ?: return LikelyApp.NothingOnScreen
        val age = kotlin.math.abs(atMs - since)
        val confidence = when {
            age <= HIGH_WINDOW_MS -> Confidence.HIGH
            age <= MEDIUM_WINDOW_MS -> Confidence.MEDIUM
            else -> Confidence.LOW
        }
        return LikelyApp(app, confidence, inForeground = true)
    }
}
