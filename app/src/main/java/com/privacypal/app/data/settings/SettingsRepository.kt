package com.privacypal.app.data.settings

import com.privacypal.app.domain.model.ThemeMode
import com.privacypal.app.domain.model.ThemeSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeSettings: Flow<ThemeSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)
}
