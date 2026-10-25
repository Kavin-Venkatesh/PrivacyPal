package com.privacypal.app.di

import android.content.Context
import androidx.room.Room
import com.privacypal.app.data.apps.Clock
import com.privacypal.app.data.db.AppDao
import com.privacypal.app.data.db.AppRuleDao
import com.privacypal.app.data.db.PrivacyPalDatabase
import com.privacypal.app.data.db.SensorEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PrivacyPalDatabase =
        Room.databaseBuilder(context, PrivacyPalDatabase::class.java, PrivacyPalDatabase.NAME).build()

    @Provides
    fun provideAppDao(database: PrivacyPalDatabase): AppDao = database.appDao()

    @Provides
    fun provideSensorEventDao(database: PrivacyPalDatabase): SensorEventDao = database.sensorEventDao()

    @Provides
    fun provideAppRuleDao(database: PrivacyPalDatabase): AppRuleDao = database.appRuleDao()

    @Provides
    fun provideClock(): Clock = Clock { System.currentTimeMillis() }
}
