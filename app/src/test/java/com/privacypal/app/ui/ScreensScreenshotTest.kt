package com.privacypal.app.ui

import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.privacypal.app.domain.model.ThemeMode
import com.privacypal.app.domain.model.ThemeSettings
import com.privacypal.app.testing.InAppShell
import com.privacypal.app.testing.captureLightAndDark
import com.privacypal.app.ui.activity.ActivityScreen
import com.privacypal.app.ui.home.HomeScreen
import com.privacypal.app.ui.home.HomeUiState
import com.privacypal.app.ui.navigation.TopLevelDestination
import com.privacypal.app.ui.settings.SettingsScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScreensScreenshotTest {

    @Test
    fun home() = captureLightAndDark("home") {
        InAppShell(TopLevelDestination.HOME) { HomeScreen(HomeUiState.Empty, it) }
    }

    @Test
    fun activity() = captureLightAndDark("activity") {
        InAppShell(TopLevelDestination.ACTIVITY) { ActivityScreen(it) }
    }

    @Test
    fun settings() = captureLightAndDark("settings") {
        InAppShell(TopLevelDestination.SETTINGS) {
            SettingsScreen(
                themeSettings = ThemeSettings(ThemeMode.SYSTEM, dynamicColor = true),
                showDynamicColor = true,
                versionName = "0.0.1",
                onThemeModeChange = {},
                onDynamicColorChange = {},
                modifier = it,
            )
        }
    }

    /** Android 10/11: no dynamic colour row; also covers a non-default selection. */
    @Test
    fun settingsWithoutDynamicColor() = captureLightAndDark("settings_api29") {
        InAppShell(TopLevelDestination.SETTINGS) {
            SettingsScreen(
                themeSettings = ThemeSettings(ThemeMode.DARK, dynamicColor = false),
                showDynamicColor = false,
                versionName = "0.0.1",
                onThemeModeChange = {},
                onDynamicColorChange = {},
                modifier = it,
            )
        }
    }
}
