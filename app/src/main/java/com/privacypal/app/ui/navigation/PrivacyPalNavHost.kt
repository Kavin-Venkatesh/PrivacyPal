package com.privacypal.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import com.privacypal.app.ui.activity.ActivityRoute as ActivityScreenRoute
import com.privacypal.app.ui.apps.AppsRoute as AppsScreenRoute
import com.privacypal.app.ui.apps.detail.AppDetailRoute as AppDetailScreenRoute
import com.privacypal.app.ui.home.HomeRoute as HomeScreenRoute
import com.privacypal.app.ui.settings.SettingsRoute as SettingsScreenRoute
import com.privacypal.app.ui.watchdog.UsageAccessRoute as UsageAccessScreenRoute

@Composable
fun PrivacyPalNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> {
            HomeScreenRoute(
                onAppClick = { navController.navigate(AppDetailRoute(it)) },
                onSeeAllApps = { navController.navigateToTopLevel(TopLevelDestination.APPS) },
                onSetUpUsageAccess = { navController.navigate(UsageAccessRoute) },
            )
        }
        navigation<AppsGraph>(startDestination = AppsRoute) {
            composable<AppsRoute> {
                AppsScreenRoute(onAppClick = { navController.navigate(AppDetailRoute(it)) })
            }
            composable<AppDetailRoute> {
                AppDetailScreenRoute(onBack = { navController.popBackStack() })
            }
        }
        composable<ActivityRoute>(
            deepLinks = listOf(navDeepLink<ActivityRoute>(basePath = ActivityRoute.DEEP_LINK)),
        ) { ActivityScreenRoute(onAppClick = { navController.navigate(AppDetailRoute(it)) }) }
        navigation<SettingsGraph>(startDestination = SettingsRoute) {
            composable<SettingsRoute> {
                SettingsScreenRoute(onUsageAccessClick = { navController.navigate(UsageAccessRoute) })
            }
            composable<UsageAccessRoute> { UsageAccessScreenRoute(onBack = { navController.popBackStack() }) }
        }
    }
}
