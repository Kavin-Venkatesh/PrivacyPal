package com.privacypal.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.privacypal.app.data.apps.Clock
import com.privacypal.app.data.watchdog.SensorUseRepository
import com.privacypal.app.data.watchdog.WatchdogSettingsRepository
import com.privacypal.app.domain.watchdog.HistoryRetention
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/** Deletes mic and camera history older than the user's retention setting. Runs once a day. */
@HiltWorker
class HistoryCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settings: WatchdogSettingsRepository,
    private val repository: SensorUseRepository,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val days = settings.retentionDays.first()
        repository.deleteStartedBefore(HistoryRetention.cutoff(clock.now(), days))
        return Result.success()
    }

    companion object {
        private const val NAME = "history-cleanup"

        /** Schedules the daily run; keeps an existing schedule. */
        fun schedule(workManager: WorkManager) {
            workManager.enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<HistoryCleanupWorker>(1, TimeUnit.DAYS).build(),
            )
        }
    }
}
