package com.privacypal.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.privacypal.app.MainActivity
import com.privacypal.app.R

/** Notification channels and notifications of the mic and camera watchdog. */
object WatchdogNotifications {
    const val STATUS_CHANNEL_ID = "watchdog_status"
    const val STATUS_NOTIFICATION_ID = 1

    fun createChannels(context: Context) {
        val status = NotificationChannel(
            STATUS_CHANNEL_ID,
            context.getString(R.string.watchdog_channel_status),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.watchdog_channel_status_description)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(status)
    }

    /** The ongoing notification Android requires while the watchdog runs. */
    fun status(context: Context): Notification = NotificationCompat.Builder(context, STATUS_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_watchdog)
        .setContentTitle(context.getString(R.string.watchdog_status_title))
        .setContentText(context.getString(R.string.watchdog_status_text))
        .setContentIntent(openApp(context))
        .setOngoing(true)
        .setSilent(true)
        .setShowWhen(false)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
