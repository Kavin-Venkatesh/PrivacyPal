package com.privacypal.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {

    @Test
    fun riskColorsMeetContrastInLightTheme() {
        assertRiskContrast(LightPrivacyPalColors, LightColorScheme.surface, "light")
    }

    @Test
    fun riskColorsMeetContrastInDarkTheme() {
        assertRiskContrast(DarkPrivacyPalColors, DarkColorScheme.surface, "dark")
    }

    @Test
    fun brandTextPairsMeetContrastInLightTheme() {
        assertBrandContrast(LightColorScheme, "light")
    }

    @Test
    fun brandTextPairsMeetContrastInDarkTheme() {
        assertBrandContrast(DarkColorScheme, "dark")
    }

    @Test
    fun contrastRatioMatchesKnownValues() {
        assertTrue(contrastRatio(Color.Black, Color.White) in 20.9..21.1)
        assertTrue(contrastRatio(Color.White, Color.White) in 0.99..1.01)
    }

    private fun assertRiskContrast(colors: PrivacyPalColors, surface: Color, theme: String) {
        listOf(
            Triple("riskHigh", colors.riskHigh, colors.onRiskHigh),
            Triple("riskMedium", colors.riskMedium, colors.onRiskMedium),
            Triple("riskLow", colors.riskLow, colors.onRiskLow),
        ).forEach { (name, risk, onRisk) ->
            assertMinContrast("$theme $name / on$name", risk, onRisk)
            assertMinContrast("$theme $name / surface", risk, surface)
        }
    }

    private fun assertBrandContrast(scheme: ColorScheme, theme: String) {
        listOf(
            "primary" to (scheme.primary to scheme.onPrimary),
            "primaryContainer" to (scheme.primaryContainer to scheme.onPrimaryContainer),
            "secondary" to (scheme.secondary to scheme.onSecondary),
            "secondaryContainer" to (scheme.secondaryContainer to scheme.onSecondaryContainer),
            "tertiary" to (scheme.tertiary to scheme.onTertiary),
            "tertiaryContainer" to (scheme.tertiaryContainer to scheme.onTertiaryContainer),
            "error" to (scheme.error to scheme.onError),
            "errorContainer" to (scheme.errorContainer to scheme.onErrorContainer),
            "surface" to (scheme.surface to scheme.onSurface),
            "surface/onSurfaceVariant" to (scheme.surface to scheme.onSurfaceVariant),
            "surfaceContainer/onSurfaceVariant" to (scheme.surfaceContainer to scheme.onSurfaceVariant),
            "surfaceContainerHighest" to (scheme.surfaceContainerHighest to scheme.onSurface),
            "inverseSurface" to (scheme.inverseSurface to scheme.inverseOnSurface),
            "surface/primary" to (scheme.surface to scheme.primary),
        ).forEach { (name, pair) ->
            assertMinContrast("$theme $name", pair.first, pair.second)
        }
    }

    private fun assertMinContrast(label: String, a: Color, b: Color) {
        val ratio = contrastRatio(a, b)
        assertTrue("$label contrast %.2f is below $MIN_CONTRAST".format(ratio), ratio >= MIN_CONTRAST)
    }

    /** WCAG 2.x contrast ratio. */
    private fun contrastRatio(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return (maxOf(la, lb) / minOf(la, lb)).toDouble()
    }

    private companion object {
        const val MIN_CONTRAST = 4.5
    }
}
