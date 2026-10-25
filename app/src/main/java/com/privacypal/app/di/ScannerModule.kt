package com.privacypal.app.di

import com.privacypal.app.data.scanner.AppIconLoader
import com.privacypal.app.data.scanner.InstalledAppsSource
import com.privacypal.app.data.scanner.PackageManagerAppIconLoader
import com.privacypal.app.data.scanner.PackageManagerAppsSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ScannerModule {

    @Binds
    abstract fun bindInstalledAppsSource(impl: PackageManagerAppsSource): InstalledAppsSource

    @Binds
    abstract fun bindAppIconLoader(impl: PackageManagerAppIconLoader): AppIconLoader
}
