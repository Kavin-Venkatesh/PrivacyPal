package com.privacypal.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.privacypal.app.ui.theme.PrivacyPalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme = isSystemInDarkTheme()
            SystemBarsEffect(darkTheme)
            PrivacyPalTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.safeDrawingPadding(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
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
