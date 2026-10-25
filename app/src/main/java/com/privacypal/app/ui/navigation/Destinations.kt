package com.privacypal.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.ui.graphics.vector.ImageVector
import com.privacypal.app.R
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object HomeRoute

@Serializable
data object AppsRoute

@Serializable
data object ActivityRoute

@Serializable
data object SettingsRoute

enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val labelRes: Int,
) {
    HOME(HomeRoute, HomeRoute::class, Icons.Filled.Home, Icons.Outlined.Home, R.string.nav_home),
    APPS(AppsRoute, AppsRoute::class, Icons.Filled.Apps, Icons.Outlined.Apps, R.string.nav_apps),
    ACTIVITY(
        ActivityRoute,
        ActivityRoute::class,
        Icons.Filled.Timeline,
        Icons.Outlined.Timeline,
        R.string.nav_activity,
    ),
    SETTINGS(
        SettingsRoute,
        SettingsRoute::class,
        Icons.Filled.Settings,
        Icons.Outlined.Settings,
        R.string.nav_settings,
    ),
}
