package com.privacypal.app.domain.theme

import com.privacypal.app.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResolverTest {

    @Test
    fun systemModeFollowsSystemSetting() {
        assertTrue(resolveDarkTheme(ThemeMode.SYSTEM, systemIsDark = true))
        assertFalse(resolveDarkTheme(ThemeMode.SYSTEM, systemIsDark = false))
    }

    @Test
    fun lightModeOverridesDarkSystem() {
        assertFalse(resolveDarkTheme(ThemeMode.LIGHT, systemIsDark = true))
        assertFalse(resolveDarkTheme(ThemeMode.LIGHT, systemIsDark = false))
    }

    @Test
    fun darkModeOverridesLightSystem() {
        assertTrue(resolveDarkTheme(ThemeMode.DARK, systemIsDark = false))
        assertTrue(resolveDarkTheme(ThemeMode.DARK, systemIsDark = true))
    }

    @Test
    fun dynamicColorRequiresAndroid12() {
        assertEquals(31, DYNAMIC_COLOR_MIN_SDK)
        assertFalse(shouldUseDynamicColor(enabled = true, sdkInt = 29))
        assertFalse(shouldUseDynamicColor(enabled = true, sdkInt = 30))
        assertTrue(shouldUseDynamicColor(enabled = true, sdkInt = 31))
        assertTrue(shouldUseDynamicColor(enabled = true, sdkInt = 37))
    }

    @Test
    fun dynamicColorOffWhenUserDisablesIt() {
        assertFalse(shouldUseDynamicColor(enabled = false, sdkInt = 31))
        assertFalse(shouldUseDynamicColor(enabled = false, sdkInt = 29))
    }
}
