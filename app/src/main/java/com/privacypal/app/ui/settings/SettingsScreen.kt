package com.privacypal.app.ui.settings

import android.os.Build
import androidx.annotation.StringRes
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privacypal.app.BuildConfig
import com.privacypal.app.R
import com.privacypal.app.domain.model.ThemeMode
import com.privacypal.app.domain.model.ThemeSettings
import com.privacypal.app.domain.theme.supportsDynamicColor
import com.privacypal.app.ui.components.ScreenTitle

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val themeSettings by viewModel.themeSettings.collectAsStateWithLifecycle()
    SettingsScreen(
        themeSettings = themeSettings,
        showDynamicColor = supportsDynamicColor(Build.VERSION.SDK_INT),
        versionName = BuildConfig.VERSION_NAME,
        onThemeModeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setDynamicColor,
    )
}

@Composable
fun SettingsScreen(
    themeSettings: ThemeSettings?,
    showDynamicColor: Boolean,
    versionName: String,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
    ) {
        ScreenTitle(
            text = stringResource(R.string.settings_title),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        SectionHeader(R.string.settings_section_appearance)
        if (themeSettings != null) {
            ThemeSelector(selected = themeSettings.themeMode, onSelect = onThemeModeChange)
            if (showDynamicColor) {
                DynamicColorRow(checked = themeSettings.dynamicColor, onCheckedChange = onDynamicColorChange)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
        SectionHeader(R.string.settings_section_about)
        AboutRow(title = stringResource(R.string.settings_version), value = versionName)
        AboutRow(
            title = stringResource(R.string.settings_licence),
            value = stringResource(R.string.settings_licence_value),
        )
        OnDeviceRow()
    }
}

@Composable
private fun SectionHeader(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Text(
        text = stringResource(R.string.settings_theme),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    Column(Modifier.selectableGroup()) {
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = mode == selected, onClick = null)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(mode.labelRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun DynamicColorRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_dynamic_color),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_dynamic_color_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun AboutRow(title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OnDeviceRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = stringResource(R.string.settings_on_device_icon),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = stringResource(R.string.settings_on_device_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_on_device_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}
