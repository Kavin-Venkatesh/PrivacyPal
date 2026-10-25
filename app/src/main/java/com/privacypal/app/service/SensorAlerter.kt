package com.privacypal.app.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.privacypal.app.MainActivity
import com.privacypal.app.R
import com.privacypal.app.data.scanner.AppIconLoader
import com.privacypal.app.data.watchdog.AppRuleRepository
import com.privacypal.app.data.watchdog.SensorUseRepository
import com.privacypal.app.domain.watchdog.AlertLevel
import com.privacypal.app.domain.watchdog.AlertRules
import com.privacypal.app.domain.watchdog.Sensor
import com.privacypal.app.domain.watchdog.SensorUse
import com.privacypal.app.ui.navigation.ActivityRoute
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Tells the user about microphone and camera use. Call from one thread (the service's main scope). */
interface SensorAlerter {
    /** The likely app of use [id] is stored: alert if [AlertRules] says so. */
    suspend fun onLikelyAppKnown(id: Long)

    /** Use [id] ended: an alert still on screen is updated with how long it lasted. */
    suspend fun onEnded(id: Long)

    /** Use [id] turned out not to be real (e.g. the flashlight). */
    fun cancel(id: Long)
}

@Singleton
class NotificationSensorAlerter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SensorUseRepository,
    private val rules: AppRuleRepository,
    private val iconLoader: AppIconLoader,
) : SensorAlerter {

    private val manager = NotificationManagerCompat.from(context)

    /** Level of each alert posted, so the update on end uses the same channel. */
    private val posted = mutableMapOf<Long, AlertLevel>()

    override suspend fun onLikelyAppKnown(id: Long) {
        val use = repository.get(id) ?: return
        val level = AlertRules.level(use, rules.mutedPackages.first())
        if (level == AlertLevel.NONE) return
        if (post(id, build(use, level))) posted[id] = level
    }

    override suspend fun onEnded(id: Long) {
        val level = posted.remove(id)
        // Don't bring back an alert the user already dismissed.
        val showing = manager.activeNotifications.any { it.id == alertId(id) }
        val use = if (level != null && showing) repository.get(id) else null
        if (level != null && use != null) post(id, build(use, level))
    }

    override fun cancel(id: Long) {
        posted.remove(id)
        manager.cancel(alertId(id))
    }

    /** Returns whether the alert could be shown (notifications may be off). */
    private fun post(id: Long, notification: Notification): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        manager.notify(alertId(id), notification)
        return true
    }

    private suspend fun build(use: SensorUse, level: AlertLevel): Notification {
        val packageName = use.likelyApp?.packageName
        val label = packageName?.let(::labelOf)
        val text = alertText(use, label)
        val urgent = level == AlertLevel.URGENT
        val channel = if (urgent) WatchdogNotifications.URGENT_CHANNEL_ID else WatchdogNotifications.ALERTS_CHANNEL_ID
        val priority = if (urgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_watchdog)
            .setContentTitle(alertTitle(use))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setLargeIcon(packageName?.let { iconLoader.load(it)?.asAndroidBitmap() })
            .setWhen(use.startedAtMs)
            .setShowWhen(true)
            .setContentIntent(openUse(use.id))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(priority)
        if (packageName != null && label != null) {
            builder.addAction(0, context.getString(R.string.alert_mute, label), muteApp(use.id, packageName))
        }
        return builder.build()
    }

    private fun alertTitle(use: SensorUse): String {
        val mic = use.sensor == Sensor.MICROPHONE
        val endedAt = use.endedAtMs ?: return context.getString(
            if (mic) R.string.alert_mic_in_use else R.string.alert_camera_in_use,
        )
        val duration = formatDuration(endedAt - use.startedAtMs)
        return context.getString(if (mic) R.string.alert_mic_used else R.string.alert_camera_used, duration)
    }

    private fun alertText(use: SensorUse, label: String?): String {
        val who = when {
            label != null -> context.getString(R.string.alert_likely, label)
            use.likelyApp?.inForeground == false -> context.getString(R.string.alert_background)
            else -> context.getString(R.string.alert_unknown)
        }
        return if (use.screenOn) who else "$who ${context.getString(R.string.alert_screen_off)}"
    }

    private fun formatDuration(ms: Long): String {
        val seconds = ((ms + MS_PER_SECOND / 2) / MS_PER_SECOND).toInt().coerceAtLeast(1)
        return if (seconds < SECONDS_PER_MINUTE) {
            context.resources.getQuantityString(R.plurals.duration_seconds, seconds, seconds)
        } else {
            val minutes = seconds / SECONDS_PER_MINUTE
            context.resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
        }
    }

    private fun labelOf(packageName: String): String = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }

    private fun openUse(id: Long): PendingIntent = PendingIntent.getActivity(
        context,
        id.toInt(),
        Intent(Intent.ACTION_VIEW, "${ActivityRoute.DEEP_LINK}?eventId=$id".toUri(), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun muteApp(id: Long, packageName: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        id.toInt(),
        Intent(context, MuteAlertsReceiver::class.java)
            .putExtra(MuteAlertsReceiver.EXTRA_PACKAGE, packageName)
            .putExtra(MuteAlertsReceiver.EXTRA_EVENT_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val MS_PER_SECOND = 1_000L
        private const val SECONDS_PER_MINUTE = 60
        private const val ALERT_ID_BASE = 1_000

        /** Notification id of the alert for use [id]; the status notification is 1. */
        fun alertId(id: Long): Int = ALERT_ID_BASE + id.toInt()
    }
}
