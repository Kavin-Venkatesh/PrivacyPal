package com.privacypal.app

import android.app.Application
import com.privacypal.app.data.apps.AppsRefresher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PrivacyPalApplication : Application() {

    @Inject
    lateinit var appsRefresher: AppsRefresher

    override fun onCreate() {
        super.onCreate()
        appsRefresher.start()
    }
}
