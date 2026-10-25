package com.privacypal.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.privacypal.app.data.watchdog.AppRuleRepository
import com.privacypal.app.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** The "Don't alert for <app>" button on an alert. Not exported: only PrivacyPal's own notifications. */
@AndroidEntryPoint
class MuteAlertsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var rules: AppRuleRepository

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1)
        NotificationManagerCompat.from(context).cancel(NotificationSensorAlerter.alertId(eventId))
        val pending = goAsync()
        scope.launch {
            try {
                rules.setMuted(packageName, muted = true)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE = "com.privacypal.app.extra.PACKAGE"
        const val EXTRA_EVENT_ID = "com.privacypal.app.extra.EVENT_ID"
    }
}
