package com.privacypal.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.privacypal.app.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Restarts the watchdog after the phone boots or PrivacyPal is updated, if the user turned it on.
 * Both broadcasts let an app start a specialUse foreground service from the background.
 */
@AndroidEntryPoint
class WatchdogStartReceiver : BroadcastReceiver() {

    @Inject
    lateinit var controller: WatchdogController

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        // Exported for the system broadcasts; other apps can't send these protected actions.
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val pending = goAsync()
        scope.launch {
            try {
                controller.startIfEnabled()
            } finally {
                pending.finish()
            }
        }
    }
}
