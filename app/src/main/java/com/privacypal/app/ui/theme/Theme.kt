package com.privacypal.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

/**
 * PrivacyPal theme. Uses wallpaper-based dynamic colour on Android 12+ when [dynamicColor] is
 * true, otherwise the brand scheme. Risk colours always come from the brand set so their meaning
 * and contrast stay fixed.
 */
@Composable
fun PrivacyPalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme

        else -> LightColorScheme
    }
    val privacyPalColors = if (darkTheme) DarkPrivacyPalColors else LightPrivacyPalColors

    CompositionLocalProvider(LocalPrivacyPalColors provides privacyPalColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PrivacyPalTypography,
            shapes = PrivacyPalShapes,
            content = content,
        )
    }
}
