package com.privacypal.app.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.privacypal.app.data.watchdog.WatchdogSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Starts and stops [WatchdogService] to match the user's "Watch mic and camera" setting. */
@Singleton
class WatchdogController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: WatchdogSettingsRepository,
) {
    val enabled: Flow<Boolean> = settings.enabled

    suspend fun setEnabled(enabled: Boolean) {
        settings.setEnabled(enabled)
        if (enabled) start() else stop()
    }

    /**
     * Starts the service if the user turned it on. Only call while PrivacyPal may start a foreground
     * service: from the visible app, or after boot or an app update.
     */
    suspend fun startIfEnabled() {
        if (settings.enabled.first()) start()
    }

    private fun start() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, WatchdogService::class.java))
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException (Android 12+) when started from the background.
            Log.w(TAG, "Could not start the watchdog", e)
        }
    }

    private fun stop() {
        context.stopService(Intent(context, WatchdogService::class.java))
    }

    private companion object {
        const val TAG = "PrivacyPalWatchdog"
    }
}
