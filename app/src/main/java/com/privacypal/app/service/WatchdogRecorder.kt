package com.privacypal.app.service

import com.privacypal.app.data.apps.Clock
import com.privacypal.app.data.watchdog.ForegroundAppResolver
import com.privacypal.app.data.watchdog.ScreenState
import com.privacypal.app.data.watchdog.SensorMonitors
import com.privacypal.app.data.watchdog.SensorUseRepository
import com.privacypal.app.domain.watchdog.Sensor
import com.privacypal.app.domain.watchdog.SensorSessionTracker
import com.privacypal.app.domain.watchdog.SessionChange
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Turns monitor signals into stored [com.privacypal.app.domain.watchdog.SensorUse] records: a row
 * when a sensor starts, the likely app a few seconds later, and the end time when it stops.
 */
class WatchdogRecorder @Inject constructor(
    private val monitors: SensorMonitors,
    private val resolver: ForegroundAppResolver,
    private val repository: SensorUseRepository,
    private val screen: ScreenState,
    private val clock: Clock,
    private val alerter: SensorAlerter,
) {
    /** Runs until cancelled; uses still open are closed at the moment of cancelling. */
    suspend fun run() = coroutineScope {
        repository.markOpenInterrupted()
        val tracker = SensorSessionTracker()
        val openIds = mutableMapOf<Sensor, Long>()
        try {
            monitors.events.collect { event ->
                when (val change = tracker.onEvent(event)) {
                    is SessionChange.Started -> {
                        val session = change.session
                        val id = repository.recordStart(session, screenOn = screen.isOn())
                        openIds[session.sensor] = id
                        // The resolver waits a few seconds; don't hold up the next signal.
                        launch {
                            repository.recordLikelyApp(id, resolver.likelyAppAt(session.startedAtMs))
                            alerter.onLikelyAppKnown(id)
                        }
                    }

                    is SessionChange.Ended -> openIds.remove(change.session.sensor)?.let { id ->
                        repository.recordEnd(id, checkNotNull(change.session.endedAtMs))
                        alerter.onEnded(id)
                    }

                    is SessionChange.Discarded -> openIds.remove(change.session.sensor)?.let { id ->
                        repository.discard(id)
                        alerter.cancel(id)
                    }

                    null -> Unit
                }
            }
        } finally {
            withContext(NonCancellable) {
                val now = clock.now()
                openIds.values.forEach { repository.recordEnd(it, now) }
            }
        }
    }
}
