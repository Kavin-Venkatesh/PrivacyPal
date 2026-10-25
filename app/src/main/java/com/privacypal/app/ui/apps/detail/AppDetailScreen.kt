package com.privacypal.app.ui.apps.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privacypal.app.R
import com.privacypal.app.domain.apps.RatedApp
import com.privacypal.app.domain.risk.GrantRisk
import com.privacypal.app.domain.risk.RiskLevel
import com.privacypal.app.ui.components.AppIcon
import com.privacypal.app.ui.components.EmptyState
import com.privacypal.app.ui.components.RiskBadge
import com.privacypal.app.ui.risk.icon
import com.privacypal.app.ui.risk.labelRes
import com.privacypal.app.ui.risk.textRes
import com.privacypal.app.ui.watchdog.SensorUseRow
import java.text.DateFormat
import java.util.Date

@Composable
fun AppDetailRoute(onBack: () -> Unit, viewModel: AppDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sensorHistory by viewModel.sensorHistory.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Permission changes in Settings send no broadcast, so rescan whenever the user returns.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    AppDetailScreen(
        state = state,
        onBack = onBack,
        onManagePermissions = { context.openManagePermissions(viewModel.packageName) },
        sensorHistory = sensorHistory,
        onAlertsChange = viewModel::setAlertsOn,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    state: AppDetailUiState,
    onBack: () -> Unit,
    onManagePermissions: () -> Unit,
    modifier: Modifier = Modifier,
    /** Null hides the mic and camera section. */
    sensorHistory: AppSensorHistory? = null,
    onAlertsChange: (Boolean) -> Unit = {},
) {
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                }
            },
        )
        when (state) {
            AppDetailUiState.Loading -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            AppDetailUiState.NotInstalled -> NotInstalled(onBack)

            is AppDetailUiState.Content -> DetailContent(state.rated, onManagePermissions) {
                sensorHistory?.let { SensorHistorySection(state.rated.app.label, it, onAlertsChange) }
            }
        }
    }
}

@Composable
private fun DetailContent(rated: RatedApp, onManagePermissions: () -> Unit, extra: @Composable () -> Unit) {
    val app = rated.app
    val denied = app.permissions
        .filter { !it.granted }
        .map { it.group }
        .distinct()
        .filter { it !in app.grantedGroups }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Header(rated)
        Button(
            onClick = onManagePermissions,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.detail_manage_permissions))
        }
        Text(
            text = stringResource(R.string.detail_manage_permissions_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        SectionTitle(stringResource(R.string.detail_section_granted))
        if (rated.risk.grants.isEmpty()) {
            Text(
                text = stringResource(R.string.detail_no_permissions),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        rated.risk.grants.forEach { GrantRow(it) }
        if (denied.isNotEmpty()) {
            SectionTitle(stringResource(R.string.detail_section_denied))
            Text(
                text = denied.map { stringResource(it.labelRes()) }.joinToString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        extra()
    }
}

@Composable
private fun SensorHistorySection(label: String, history: AppSensorHistory, onAlertsChange: (Boolean) -> Unit) {
    SectionTitle(stringResource(R.string.detail_section_sensor_history))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = history.alertsOn, onValueChange = onAlertsChange, role = Role.Switch)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.detail_sensor_alerts),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.detail_sensor_alerts_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = history.alertsOn, onCheckedChange = null)
    }
    if (history.recent.isEmpty()) {
        Text(
            text = stringResource(R.string.detail_sensor_history_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    history.recent.forEach { use ->
        SensorUseRow(use = use, appLabel = label)
        HorizontalDivider(Modifier.padding(start = 72.dp))
    }
}

@Composable
private fun Header(rated: RatedApp) {
    val app = rated.app
    val locale = LocalConfiguration.current.locales[0]
    val installed = remember(app.firstInstallTime, locale) {
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(app.firstInstallTime))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = app.packageName, contentDescription = app.label, size = 56.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.detail_installed, installed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (rated.risk.level != RiskLevel.NONE) {
                RiskBadge(rated.risk.level, Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun GrantRow(grant: GrantRisk) {
    val label = stringResource(grant.group.labelRes())
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = grant.group.icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    RiskBadge(grant.level)
                }
                Text(
                    text = stringResource(grant.reason.textRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        HorizontalDivider(Modifier.padding(start = 56.dp))
    }
}

@Composable
private fun NotInstalled(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyState(
            icon = Icons.Outlined.DeleteOutline,
            iconDescription = stringResource(R.string.detail_not_installed_icon),
            title = stringResource(R.string.detail_not_installed_title),
            message = stringResource(R.string.detail_not_installed_message),
        )
        Spacer(Modifier.size(16.dp))
        Button(onClick = onBack) { Text(stringResource(R.string.detail_back)) }
    }
}
