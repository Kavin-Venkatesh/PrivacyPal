package com.privacypal.app.ui.activity

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privacypal.app.R
import com.privacypal.app.domain.watchdog.Sensor
import com.privacypal.app.ui.components.EmptyState
import com.privacypal.app.ui.components.EmptyStateScreen
import com.privacypal.app.ui.components.ScreenTitle
import com.privacypal.app.ui.watchdog.SensorUseRow
import com.privacypal.app.ui.watchdog.dayText
import com.privacypal.app.ui.watchdog.labelRes
import java.time.LocalDate

@Composable
fun ActivityRoute(onAppClick: (String) -> Unit, viewModel: ActivityViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ActivityScreen(
        state = state,
        actions = ActivityActions(
            onToggleSensor = viewModel::toggleSensor,
            onToggleScreenOff = viewModel::toggleScreenOff,
            onSelectApp = viewModel::selectApp,
            onClearFilters = viewModel::clearFilters,
            onAppClick = onAppClick,
        ),
    )
}

@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier,
    state: ActivityUiState = ActivityUiState.Empty(watchdogOn = true),
    actions: ActivityActions = ActivityActions(),
) {
    when (state) {
        ActivityUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is ActivityUiState.Empty -> EmptyStateScreen(
            title = stringResource(R.string.activity_title),
            icon = Icons.Outlined.Timeline,
            iconDescription = stringResource(R.string.activity_empty_icon),
            emptyTitle = stringResource(R.string.activity_empty_title),
            emptyMessage = stringResource(
                if (state.watchdogOn) R.string.activity_empty_message else R.string.activity_watchdog_off_message,
            ),
            modifier = modifier,
        )

        is ActivityUiState.Content -> Timeline(state, actions, modifier)
    }
}

@Composable
private fun Timeline(state: ActivityUiState.Content, actions: ActivityActions, modifier: Modifier) {
    val listState = rememberLazyListState()
    val today = remember { LocalDate.now() }
    // Scroll to the use an alert was opened for, once.
    var scrolledToHighlight by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.highlightId, state.days) {
        val index = state.indexOf(state.highlightId)
        if (!scrolledToHighlight && index != null) {
            listState.scrollToItem(index)
            scrolledToHighlight = true
        }
    }
    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        item(key = "title") {
            ScreenTitle(
                text = stringResource(R.string.activity_title),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
            )
        }
        item(key = "filters") { Filters(state, actions) }
        if (state.days.isEmpty()) {
            item(key = "no-match") { NoMatch(actions.onClearFilters) }
        }
        state.days.forEach { group ->
            item(key = "day-${group.day}") {
                Text(
                    text = dayText(group.day, today),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                        .semantics { heading() },
                )
            }
            items(group.uses, key = { it.id }) { use ->
                val packageName = use.likelyApp?.packageName
                Column {
                    SensorUseRow(
                        use = use,
                        appLabel = packageName?.let { state.labels[it] },
                        highlighted = use.id == state.highlightId,
                        onClick = packageName?.let { { actions.onAppClick(it) } },
                    )
                    HorizontalDivider(Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

/** List index of use [id]: after the title and filters, each day is a header plus its uses. */
private fun ActivityUiState.Content.indexOf(id: Long?): Int? {
    // null marks a day header row.
    val rows = days.flatMap { group -> listOf(null) + group.uses.map { it.id } }
    return rows.indexOf(id).takeIf { id != null && it >= 0 }?.plus(LEADING_ITEMS)
}

/** Title and filters. */
private const val LEADING_ITEMS = 2

@Composable
private fun Filters(state: ActivityUiState.Content, actions: ActivityActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Sensor.entries.forEach { sensor ->
            FilterChip(
                selected = sensor in state.filter.sensors,
                onClick = { actions.onToggleSensor(sensor) },
                label = { Text(stringResource(sensor.labelRes)) },
                leadingIcon = selectedIcon(sensor in state.filter.sensors),
            )
        }
        FilterChip(
            selected = state.filter.screenOffOnly,
            onClick = actions.onToggleScreenOff,
            label = { Text(stringResource(R.string.activity_filter_screen_off)) },
            leadingIcon = selectedIcon(state.filter.screenOffOnly),
        )
        AppFilter(state, actions.onSelectApp)
    }
}

@Composable
private fun AppFilter(state: ActivityUiState.Content, onSelectApp: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.filter.packageName
    val allApps = stringResource(R.string.activity_filter_all_apps)
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(selected?.let { state.labels[it] ?: it } ?: allApps) },
            leadingIcon = selectedIcon(selected != null),
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allApps) },
                onClick = {
                    onSelectApp(null)
                    expanded = false
                },
            )
            state.apps.forEach { packageName ->
                DropdownMenuItem(
                    text = { Text(state.labels[packageName] ?: packageName) },
                    onClick = {
                        onSelectApp(packageName)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** A check mark on selected chips, so the state is not shown by colour alone. */
private fun selectedIcon(selected: Boolean): (@Composable () -> Unit)? = if (selected) {
    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
} else {
    null
}

@Composable
private fun NoMatch(onClearFilters: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = Icons.Outlined.Timeline,
            iconDescription = stringResource(R.string.activity_empty_icon),
            title = stringResource(R.string.activity_filtered_empty_title),
            message = stringResource(R.string.activity_filtered_empty_message),
        )
        TextButton(onClick = onClearFilters, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.activity_clear_filters))
        }
    }
}
