package com.privacypal.app.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps PrivacyPal running while the mic and camera watchdog is on, so Android delivers the
 * recording and camera availability callbacks (a cached app is frozen and misses them).
 * Foreground service type specialUse: it only listens to system callbacks and never records audio
 * or opens a camera, so the microphone and camera types (and their permissions) don't apply.
 */
@AndroidEntryPoint
class WatchdogService : Service() {

    @Inject
    lateinit var recorder: WatchdogRecorder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var recording: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        WatchdogNotifications.createChannels(this)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            WatchdogNotifications.STATUS_NOTIFICATION_ID,
            WatchdogNotifications.status(this),
            type,
        )
        if (recording == null) recording = scope.launch { recorder.run() }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
