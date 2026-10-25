package com.privacypal.app.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privacypal.app.R
import com.privacypal.app.domain.apps.RatedApp
import com.privacypal.app.ui.components.AppIcon
import com.privacypal.app.ui.components.EmptyStateScreen
import com.privacypal.app.ui.components.RiskBadge
import com.privacypal.app.ui.components.ScreenTitle
import com.privacypal.app.ui.risk.labelRes

@Composable
fun HomeRoute(
    onAppClick: (String) -> Unit,
    onSeeAllApps: () -> Unit,
    onSetUpUsageAccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val watchdog by viewModel.watchdog.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(context.notificationsAllowed()) }
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshUsageAccess()
        notificationsAllowed = context.notificationsAllowed()
        onPauseOrDispose {}
    }
    // Turning the watchdog on asks for notifications (Android 13+); it runs whatever the answer.
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = context.notificationsAllowed()
        viewModel.setWatchdogEnabled(true)
    }
    val actions = WatchdogActions(
        onEnabledChange = { enabled ->
            if (enabled && context.shouldAskForNotifications()) {
                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setWatchdogEnabled(enabled)
            }
        },
        onAllowNotifications = { context.openNotificationSettings() },
        onSetUpUsageAccess = onSetUpUsageAccess,
    )
    HomeScreen(
        state = state,
        watchdog = watchdog?.copy(notificationsAllowed = notificationsAllowed),
        watchdogActions = actions,
        onAppClick = onAppClick,
        onSeeAllApps = onSeeAllApps,
        modifier = modifier,
    )
}

private fun Context.notificationsAllowed() = NotificationManagerCompat.from(this).areNotificationsEnabled()

private fun Context.shouldAskForNotifications() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
    PackageManager.PERMISSION_GRANTED

private fun Context.openNotificationSettings() {
    startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    watchdog: WatchdogCardState? = null,
    watchdogActions: WatchdogActions = WatchdogActions(),
    onAppClick: (String) -> Unit = {},
    onSeeAllApps: () -> Unit = {},
) {
    when (state) {
        HomeUiState.Empty -> EmptyStateScreen(
            title = stringResource(R.string.home_title),
            icon = Icons.Outlined.Shield,
            iconDescription = stringResource(R.string.home_empty_icon),
            emptyTitle = stringResource(R.string.home_empty_title),
            emptyMessage = stringResource(R.string.home_empty_message),
            modifier = modifier,
        )

        HomeUiState.Loading -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(vertical = 24.dp),
        ) {
            ScreenTitle(stringResource(R.string.home_title), Modifier.padding(horizontal = 16.dp))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.heightIn(min = 16.dp))
                Text(
                    text = stringResource(R.string.apps_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        is HomeUiState.Content -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
        ) {
            ScreenTitle(stringResource(R.string.home_title), Modifier.padding(horizontal = 16.dp))
            watchdog?.let { WatchdogCard(it, watchdogActions) }
            PermissionCard(state, onAppClick, onSeeAllApps)
        }
    }
}

@Composable
private fun PermissionCard(state: HomeUiState.Content, onAppClick: (String) -> Unit, onSeeAllApps: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Text(
            text = stringResource(R.string.home_permissions_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .semantics { heading() },
        )
        Row(Modifier.padding(16.dp)) {
            Stat(
                value = state.appsChecked,
                label = pluralStringResource(R.plurals.home_stat_checked, state.appsChecked),
                modifier = Modifier.weight(1f),
            )
            Stat(
                value = state.highRisk,
                label = pluralStringResource(R.plurals.home_stat_high_risk, state.highRisk),
                modifier = Modifier.weight(1f),
            )
        }
        if (state.riskiest.isEmpty()) {
            Text(
                text = stringResource(R.string.home_no_risky_apps),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            Text(
                text = stringResource(R.string.home_riskiest_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .semantics { heading() },
            )
            state.riskiest.forEach { RiskyAppRow(it, onClick = { onAppClick(it.app.packageName) }) }
        }
        OutlinedButton(
            onClick = onSeeAllApps,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) { Text(stringResource(R.string.home_see_all_apps)) }
    }
}

@Composable
private fun Stat(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RiskyAppRow(rated: RatedApp, onClick: () -> Unit) {
    val description = "${rated.app.label}. ${stringResource(rated.risk.level.labelRes())}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = rated.app.packageName, contentDescription = rated.app.label)
        Spacer(Modifier.width(16.dp))
        Text(
            text = rated.app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        RiskBadge(rated.risk.level)
    }
}
