package com.privacypal.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.privacypal.app.data.watchdog.CameraMonitor
import com.privacypal.app.data.watchdog.ForegroundAppResolver
import com.privacypal.app.data.watchdog.MicMonitor
import com.privacypal.app.di.ApplicationScope
import com.privacypal.app.domain.watchdog.SensorSessionTracker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Debug builds only. Logs microphone/camera monitor events and paired sessions under the tag
 * "PrivacyPalMonitors":
 *
 * adb shell am broadcast -n com.privacypal.app/.debug.MonitorDebugReceiver -a com.privacypal.app.debug.MONITORS --ez on true
 */
class MonitorDebugReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Monitors {
        fun micMonitor(): MicMonitor

        fun cameraMonitor(): CameraMonitor

        fun resolver(): ForegroundAppResolver

        @ApplicationScope
        fun scope(): CoroutineScope
    }

    override fun onReceive(context: Context, intent: Intent) {
        job?.cancel()
        job = null
        if (!intent.getBooleanExtra("on", true)) {
            Log.i(TAG, "monitors stopped")
            return
        }
        val monitors = EntryPointAccessors.fromApplication(context, Monitors::class.java)
        val tracker = SensorSessionTracker()
        job = monitors.scope().launch {
            Log.i(TAG, "monitors started")
            merge(monitors.micMonitor().events, monitors.cameraMonitor().events).collect { event ->
                Log.i(TAG, "event $event")
                tracker.onEvent(event)?.let { change ->
                    val session = change.session
                    val likely = monitors.resolver().likelyAppAt(session.startedAtMs)
                    Log.i(TAG, "likely app $likely")
                    Log.i(
                        TAG,
                        "session ${change::class.simpleName} $session duration=${session.durationMs} short=${session.isShort}",
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "PrivacyPalMonitors"
        var job: Job? = null
    }
}
