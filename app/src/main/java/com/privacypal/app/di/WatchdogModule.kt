package com.privacypal.app.di

import com.privacypal.app.data.watchdog.AudioManagerMicMonitor
import com.privacypal.app.data.watchdog.CameraManagerCameraMonitor
import com.privacypal.app.data.watchdog.CameraMonitor
import com.privacypal.app.data.watchdog.MicMonitor
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
}
