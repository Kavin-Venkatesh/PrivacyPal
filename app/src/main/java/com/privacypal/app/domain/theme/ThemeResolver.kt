package com.privacypal.app.domain.theme

import com.privacypal.app.domain.model.ThemeMode

/** First Android API level (12, "S") with wallpaper-based dynamic colour. */
const val DYNAMIC_COLOR_MIN_SDK = 31

/** Whether the app should render dark, given the user's choice and the system setting. */
fun resolveDarkTheme(mode: ThemeMode, systemIsDark: Boolean): Boolean = when (mode) {
    ThemeMode.SYSTEM -> systemIsDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** Dynamic colour is used only when the user enabled it and the OS supports it. */
fun shouldUseDynamicColor(enabled: Boolean, sdkInt: Int): Boolean =
    enabled && supportsDynamicColor(sdkInt)

fun supportsDynamicColor(sdkInt: Int): Boolean = sdkInt >= DYNAMIC_COLOR_MIN_SDK
