package com.privacypal.app.di

import com.privacypal.app.data.watchdog.AppOpsUsageAccessChecker
import com.privacypal.app.data.watchdog.AudioManagerMicMonitor
import com.privacypal.app.data.watchdog.CameraManagerCameraMonitor
import com.privacypal.app.data.watchdog.CameraMonitor
import com.privacypal.app.data.watchdog.ForegroundAppResolver
import com.privacypal.app.data.watchdog.MicMonitor
import com.privacypal.app.data.watchdog.UsageAccessChecker
import com.privacypal.app.data.watchdog.UsageStatsForegroundAppResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WatchdogModule {

    @Binds
    abstract fun bindMicMonitor(impl: AudioManagerMicMonitor): MicMonitor

    @Binds
    abstract fun bindCameraMonitor(impl: CameraManagerCameraMonitor): CameraMonitor

    @Binds
    abstract fun bindUsageAccessChecker(impl: AppOpsUsageAccessChecker): UsageAccessChecker

    @Binds
    abstract fun bindForegroundAppResolver(impl: UsageStatsForegroundAppResolver): ForegroundAppResolver
}
