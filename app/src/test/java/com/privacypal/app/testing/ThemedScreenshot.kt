package com.privacypal.app.testing

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.privacypal.app.ui.PrivacyPalNavigationBar
import com.privacypal.app.ui.navigation.TopLevelDestination
import com.privacypal.app.ui.theme.PrivacyPalTheme

/**
 * Captures [content] twice, as `<name>_light.png` and `<name>_dark.png`, using the brand scheme
 * (dynamic colour off) so images are deterministic. [fontScale] simulates the system font size setting.
 */
fun captureLightAndDark(name: String, fontScale: Float = 1f, content: @Composable () -> Unit) {
    listOf(false to "light", true to "dark").forEach { (dark, suffix) ->
        captureRoboImage(filePath = "src/test/screenshots/${name}_$suffix.png") {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                PrivacyPalTheme(darkTheme = dark, dynamicColor = false) {
                    content()
                }
            }
        }
    }
}

/** Renders a top-level screen inside the real bottom navigation bar, as the user sees it. */
@Composable
fun InAppShell(selected: TopLevelDestination, screen: @Composable (Modifier) -> Unit) {
    Scaffold(
        bottomBar = { PrivacyPalNavigationBar(isSelected = { it == selected }, onSelect = {}) },
    ) { innerPadding ->
        screen(Modifier.padding(innerPadding))
    }
}
