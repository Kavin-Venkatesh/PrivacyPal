package com.privacypal.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.privacypal.app.data.apps.AppsRefresher
import com.privacypal.app.work.HistoryCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PrivacyPalApplication :
    Application(),
    Configuration.Provider {

    @Inject
    lateinit var appsRefresher: AppsRefresher

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // WorkManager's default initializer is removed in the manifest so workers get Hilt injection.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        appsRefresher.start()
        HistoryCleanupWorker.schedule(WorkManager.getInstance(this))
    }
}
