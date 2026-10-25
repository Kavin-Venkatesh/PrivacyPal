package com.privacypal.app.testing

import com.privacypal.app.data.settings.SettingsRepository
import com.privacypal.app.domain.model.ThemeMode
import com.privacypal.app.domain.model.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** In-memory SettingsRepository. Emits nothing until [emit] or a setter is called. */
class FakeSettingsRepository : SettingsRepository {
    private val flow = MutableSharedFlow<ThemeSettings>(replay = 1)
    private var current = ThemeSettings()

    override val themeSettings: Flow<ThemeSettings> = flow.distinctUntilChanged()

    fun emit(settings: ThemeSettings) {
        current = settings
        flow.tryEmit(settings)
    }

    override suspend fun setThemeMode(mode: ThemeMode) = emit(current.copy(themeMode = mode))

    override suspend fun setDynamicColor(enabled: Boolean) = emit(current.copy(dynamicColor = enabled))
}
