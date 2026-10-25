package com.privacypal.app.di

import com.privacypal.app.data.watchdog.AppOpsUsageAccessChecker
import com.privacypal.app.data.watchdog.AppRuleRepository
import com.privacypal.app.data.watchdog.AudioManagerMicMonitor
import com.privacypal.app.data.watchdog.CameraManagerCameraMonitor
import com.privacypal.app.data.watchdog.CameraMonitor
import com.privacypal.app.data.watchdog.DataStoreWatchdogSettingsRepository
import com.privacypal.app.data.watchdog.ForegroundAppResolver
import com.privacypal.app.data.watchdog.MicMonitor
import com.privacypal.app.data.watchdog.PowerManagerScreenState
import com.privacypal.app.data.watchdog.RoomAppRuleRepository
import com.privacypal.app.data.watchdog.RoomSensorUseRepository
import com.privacypal.app.data.watchdog.ScreenState
import com.privacypal.app.data.watchdog.SensorUseRepository
import com.privacypal.app.data.watchdog.UsageAccessChecker
import com.privacypal.app.data.watchdog.UsageStatsForegroundAppResolver
import com.privacypal.app.data.watchdog.WatchdogSettingsRepository
import com.privacypal.app.service.NotificationSensorAlerter
import com.privacypal.app.service.SensorAlerter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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

    @Binds
    abstract fun bindSensorUseRepository(impl: RoomSensorUseRepository): SensorUseRepository

    @Binds
    @Singleton
    abstract fun bindWatchdogSettings(impl: DataStoreWatchdogSettingsRepository): WatchdogSettingsRepository

    @Binds
    abstract fun bindScreenState(impl: PowerManagerScreenState): ScreenState

    @Binds
    abstract fun bindAppRuleRepository(impl: RoomAppRuleRepository): AppRuleRepository

    @Binds
    abstract fun bindSensorAlerter(impl: NotificationSensorAlerter): SensorAlerter
}
