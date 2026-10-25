package com.privacypal.app.data.watchdog

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import com.privacypal.app.di.IoDispatcher
import com.privacypal.app.domain.watchdog.LikelyApp
import com.privacypal.app.domain.watchdog.LikelyAppRules
import com.privacypal.app.domain.watchdog.UsageEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Whether the user has granted PrivacyPal usage access (Settings > Special app access > Usage access). */
fun interface UsageAccessChecker {
    fun isGranted(): Boolean
}

/** Guesses which app is using a sensor from the app on screen. See [LikelyAppRules]. */
fun interface ForegroundAppResolver {
    /**
     * May suspend for a few seconds: an app's screen is often reported just after it starts the
     * sensor, and usage events are written with a short delay.
     */
    suspend fun likelyAppAt(timeMs: Long): LikelyApp
}

class AppOpsUsageAccessChecker @Inject constructor(@ApplicationContext private val context: Context) :
    UsageAccessChecker {
    override fun isGranted(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }
}

class UsageStatsForegroundAppResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageAccess: UsageAccessChecker,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ForegroundAppResolver {

    /** Home screens, system UI and PrivacyPal itself never count as the likely app. */
    private val ignored: Set<String> by lazy {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.queryIntentActivities(home, PackageManager.MATCH_ALL)
            .mapTo(mutableSetOf()) { it.activityInfo.packageName } + setOf(SYSTEM_UI, context.packageName)
    }

    override suspend fun likelyAppAt(timeMs: Long): LikelyApp {
        if (!usageAccess.isGranted()) return LikelyApp.Unavailable
        val settleAt = timeMs + LikelyAppRules.HIGH_WINDOW_MS + SETTLE_MS
        delay(settleAt - System.currentTimeMillis())
        return withContext(ioDispatcher) {
            val usageStats = context.getSystemService(UsageStatsManager::class.java)
            val query = usageStats.queryEvents(
                timeMs - LOOKBACK_MS,
                maxOf(System.currentTimeMillis(), timeMs + LikelyAppRules.HIGH_WINDOW_MS),
            )
            val events = mutableListOf<UsageEvent>()
            val event = UsageEvents.Event()
            while (query.hasNextEvent()) {
                query.getNextEvent(event)
                val type = when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> UsageEvent.Type.RESUMED
                    UsageEvents.Event.ACTIVITY_PAUSED -> UsageEvent.Type.PAUSED
                    else -> null
                }
                type?.let { events += UsageEvent(event.packageName, it, event.timeStamp) }
            }
            LikelyAppRules.resolve(events, timeMs, ignored)
        }
    }

    private companion object {
        /** How far back to look for the app on screen. */
        const val LOOKBACK_MS = 60 * 60 * 1_000L
        const val SYSTEM_UI = "com.android.systemui"

        /** Extra wait for usage events to be written. */
        const val SETTLE_MS = 500L
    }
}
