package com.privacypal.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Risk colours that Material 3 has no role for. Each `risk*` colour meets 4.5:1 against its
 * `onRisk*` partner and against the theme surface (see ContrastTest). Never show risk by colour
 * alone: always pair it with an icon or label.
 */
@Immutable
data class PrivacyPalColors(
    val riskHigh: Color,
    val onRiskHigh: Color,
    val riskMedium: Color,
    val onRiskMedium: Color,
    val riskLow: Color,
    val onRiskLow: Color,
)

internal val LightPrivacyPalColors = PrivacyPalColors(
    riskHigh = Color(0xFFB3261E),
    onRiskHigh = Color(0xFFFFFFFF),
    riskMedium = Color(0xFF8A5100),
    onRiskMedium = Color(0xFFFFFFFF),
    riskLow = Color(0xFF1B6E2F),
    onRiskLow = Color(0xFFFFFFFF),
)

internal val DarkPrivacyPalColors = PrivacyPalColors(
    riskHigh = Color(0xFFFFB4AB),
    onRiskHigh = Color(0xFF690005),
    riskMedium = Color(0xFFFFB870),
    onRiskMedium = Color(0xFF4A2800),
    riskLow = Color(0xFF8BD88E),
    onRiskLow = Color(0xFF00390F),
)

val LocalPrivacyPalColors = staticCompositionLocalOf { LightPrivacyPalColors }

@Suppress("UnusedReceiverParameter")
val MaterialTheme.privacyPalColors: PrivacyPalColors
    @Composable
    @ReadOnlyComposable
    get() = LocalPrivacyPalColors.current
