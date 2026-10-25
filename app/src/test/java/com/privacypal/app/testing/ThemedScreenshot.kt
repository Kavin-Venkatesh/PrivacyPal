package com.privacypal.app.testing

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.takahirom.roborazzi.captureRoboImage
import com.privacypal.app.ui.PrivacyPalNavigationBar
import com.privacypal.app.ui.navigation.TopLevelDestination
import com.privacypal.app.ui.theme.PrivacyPalTheme

/**
 * Captures [content] twice, as `<name>_light.png` and `<name>_dark.png`, using the brand scheme
 * (dynamic colour off) so images are deterministic.
 */
fun captureLightAndDark(name: String, content: @Composable () -> Unit) {
    listOf(false to "light", true to "dark").forEach { (dark, suffix) ->
        captureRoboImage(filePath = "src/test/screenshots/${name}_$suffix.png") {
            PrivacyPalTheme(darkTheme = dark, dynamicColor = false) {
                content()
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
