package com.privacypal.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privacypal.app.data.scanner.AppIconLoader
import com.privacypal.app.domain.model.ThemeSettings
import com.privacypal.app.domain.theme.resolveDarkTheme
import com.privacypal.app.domain.theme.shouldUseDynamicColor
import com.privacypal.app.ui.MainUiState
import com.privacypal.app.ui.MainViewModel
import com.privacypal.app.ui.PrivacyPalApp
import com.privacypal.app.ui.components.LocalAppIconLoader
import com.privacypal.app.ui.theme.PrivacyPalTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var appIconLoader: AppIconLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash until settings load so a dark-theme user never sees a light frame.
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val settings = (uiState as? MainUiState.Ready)?.themeSettings ?: ThemeSettings()
            val darkTheme = resolveDarkTheme(settings.themeMode, isSystemInDarkTheme())
            SystemBarsEffect(darkTheme)
            PrivacyPalTheme(
                darkTheme = darkTheme,
                dynamicColor = shouldUseDynamicColor(settings.dynamicColor, Build.VERSION.SDK_INT),
            ) {
                CompositionLocalProvider(LocalAppIconLoader provides appIconLoader) {
                    PrivacyPalApp()
                }
            }
        }
    }

    /**
     * Re-applies edge-to-edge whenever the resolved theme changes, so system bar icons follow the
     * app theme (which can differ from the system setting) and stay readable.
     */
    @Composable
    private fun SystemBarsEffect(darkTheme: Boolean) {
        DisposableEffect(darkTheme) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                navigationBarStyle = SystemBarStyle.auto(LightNavScrim, DarkNavScrim) { darkTheme },
            )
            onDispose {}
        }
    }

    private companion object {
        // Scrims for 3-button navigation, matching androidx.activity defaults.
        val LightNavScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DarkNavScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
