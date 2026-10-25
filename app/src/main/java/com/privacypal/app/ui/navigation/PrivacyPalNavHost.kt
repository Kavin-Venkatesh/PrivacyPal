package com.privacypal.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.privacypal.app.ui.activity.ActivityScreen
import com.privacypal.app.ui.apps.AppsRoute as AppsScreenRoute
import com.privacypal.app.ui.apps.detail.AppDetailRoute as AppDetailScreenRoute
import com.privacypal.app.ui.home.HomeScreen
import com.privacypal.app.ui.settings.SettingsRoute as SettingsScreenRoute

@Composable
fun PrivacyPalNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> { HomeScreen() }
        navigation<AppsGraph>(startDestination = AppsRoute) {
            composable<AppsRoute> {
                AppsScreenRoute(onAppClick = { navController.navigate(AppDetailRoute(it)) })
            }
            composable<AppDetailRoute> {
                AppDetailScreenRoute(onBack = { navController.popBackStack() })
            }
        }
        composable<ActivityRoute> { ActivityScreen() }
        composable<SettingsRoute> { SettingsScreenRoute() }
    }
}
