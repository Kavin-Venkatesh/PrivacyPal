package com.privacypal.app.ui.apps

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privacypal.app.R
import com.privacypal.app.domain.apps.AppFilter
import com.privacypal.app.domain.apps.AppQuery
import com.privacypal.app.domain.apps.AppSort
import com.privacypal.app.domain.apps.AppsSummary
import com.privacypal.app.domain.apps.RatedApp
import com.privacypal.app.domain.risk.RiskLevel
import com.privacypal.app.ui.components.AppIcon
import com.privacypal.app.ui.components.EmptyState
import com.privacypal.app.ui.components.RiskBadge
import com.privacypal.app.ui.components.ScreenTitle
import com.privacypal.app.ui.risk.icon
import com.privacypal.app.ui.risk.labelRes
import kotlinx.coroutines.launch

@Composable
fun AppsRoute(
    modifier: Modifier = Modifier,
    onAppClick: ((String) -> Unit)? = null,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    AppsScreen(
        state = state,
        query = query,
        actions = AppsActions(
            onSearchChange = viewModel::onSearchChange,
            onFilterChange = viewModel::onFilterChange,
            onSortChange = viewModel::onSortChange,
            onShowSystemChange = viewModel::onShowSystemChange,
            onClearFilters = viewModel::clearFilters,
            onRetry = viewModel::retry,
            onAppClick = onAppClick,
        ),
        modifier = modifier,
    )
}

@Composable
fun AppsScreen(state: AppsUiState, query: AppQuery, actions: AppsActions, modifier: Modifier = Modifier) {
    when (state) {
        AppsUiState.Loading -> LoadingState(modifier)
        AppsUiState.Error -> ErrorState(onRetry = actions.onRetry, modifier = modifier)
        is AppsUiState.Content -> AppsList(state, query, actions, modifier)
    }
}

@Composable
private fun AppsList(state: AppsUiState.Content, query: AppQuery, actions: AppsActions, modifier: Modifier) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Title, optional banner, summary, search come before the chips.
    val filtersIndex = if (state.refreshFailed) 4 else 3
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 24.dp),
    ) {
        item(key = "title") {
            ScreenTitle(stringResource(R.string.apps_title), Modifier.padding(horizontal = 16.dp))
        }
        if (state.refreshFailed) {
            item(key = "refreshFailed") { RefreshFailedBanner(actions.onRetry) }
        }
        item(key = "summary") {
            SummaryCard(
                summary = state.summary,
                onFilterChange = { filter ->
                    actions.onFilterChange(filter)
                    // Bring the selected chip and the matching apps into view.
                    scope.launch { listState.animateScrollToItem(filtersIndex) }
                },
            )
        }
        item(key = "search") { SearchField(query.search, actions.onSearchChange) }
        item(key = "filters") { FilterChips(query.filter, actions.onFilterChange) }
        item(key = "options") {
            ListOptions(query, onSortChange = actions.onSortChange, onShowSystemChange = actions.onShowSystemChange)
        }
        if (state.rows.isEmpty()) {
            item(key = "empty") { NoAppsState(narrowed = query.isNarrowed, onClearFilters = actions.onClearFilters) }
        }
        items(state.rows, key = { it.app.packageName }) { row ->
            AppRow(row, onClick = actions.onAppClick?.let { click -> { click(row.app.packageName) } })
        }
    }
}

@Composable
private fun SearchField(search: String, onSearchChange: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = search,
        onValueChange = onSearchChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        placeholder = { Text(stringResource(R.string.apps_search_placeholder)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.apps_search_icon)) },
        trailingIcon = {
            if (search.isNotEmpty()) {
                IconButton(onClick = { onSearchChange("") }) {
                    Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.apps_search_clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )
}

@Composable
private fun FilterChips(selected: AppFilter, onFilterChange: (AppFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = { onFilterChange(filter) },
                label = { Text(stringResource(filter.labelRes())) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = stringResource(R.string.apps_filter_selected),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun ListOptions(query: AppQuery, onSortChange: (AppSort) -> Unit, onShowSystemChange: (Boolean) -> Unit) {
    Column(Modifier.padding(top = 8.dp)) {
        SortMenu(query.sort, onSortChange)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(value = query.showSystem, onValueChange = onShowSystemChange, role = Role.Switch)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.apps_show_system),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(16.dp))
            Switch(checked = query.showSystem, onCheckedChange = null)
        }
    }
}

@Composable
private fun SortMenu(sort: AppSort, onSortChange: (AppSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = 4.dp)) {
        TextButton(onClick = { expanded = true }) {
            Icon(
                Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(R.string.apps_sort_icon),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.apps_sort_current, stringResource(sort.labelRes())))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes())) },
                    onClick = {
                        expanded = false
                        onSortChange(option)
                    },
                    leadingIcon = {
                        if (option == sort) {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.apps_filter_selected))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun RefreshFailedBanner(onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.apps_refresh_failed),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) { Text(stringResource(R.string.apps_retry)) }
        }
    }
}

private data class SummaryLine(val filter: AppFilter, @PluralsRes val textRes: Int, val icon: ImageVector)

private val summaryLines = listOf(
    SummaryLine(AppFilter.HIGH_RISK, R.plurals.apps_summary_high_risk, Icons.Outlined.ErrorOutline),
    SummaryLine(AppFilter.MICROPHONE, R.plurals.apps_summary_microphone, Icons.Outlined.Mic),
    SummaryLine(AppFilter.CAMERA, R.plurals.apps_summary_camera, Icons.Outlined.CameraAlt),
    SummaryLine(AppFilter.LOCATION, R.plurals.apps_summary_location, Icons.Outlined.LocationOn),
    SummaryLine(AppFilter.CONTACTS, R.plurals.apps_summary_contacts, Icons.Outlined.Contacts),
)

/** Counts of what apps can access; tapping a line filters the list to those apps. */
@Composable
private fun SummaryCard(summary: AppsSummary, onFilterChange: (AppFilter) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Text(
            text = pluralStringResource(R.plurals.apps_summary_total, summary.total, summary.total),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )
        summaryLines.filter { summary.count(it.filter) > 0 }.forEach { line ->
            val count = summary.count(line.filter)
            val text = pluralStringResource(line.textRes, count, count)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(
                        onClickLabel = stringResource(R.string.apps_summary_show),
                        role = Role.Button,
                        onClick = { onFilterChange(line.filter) },
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = line.icon,
                    contentDescription = stringResource(line.filter.labelRes()),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clearAndSetSemantics {},
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.apps_summary_show),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun AppRow(row: RatedApp, onClick: (() -> Unit)?) {
    val permissionNames = row.risk.grants.map { stringResource(it.group.labelRes()) }
    val riskLabel = stringResource(row.risk.level.labelRes())
    val permissionsText = if (permissionNames.isEmpty()) {
        stringResource(R.string.apps_no_sensitive_permissions)
    } else {
        stringResource(R.string.apps_row_permissions, permissionNames.joinToString())
    }
    val description = listOfNotNull(
        row.app.label,
        riskLabel.takeIf { row.risk.level != RiskLevel.NONE },
        permissionsText,
    ).joinToString(". ")

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                // One announcement per row: name, risk and permissions.
                .clearAndSetSemantics { contentDescription = description }
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(packageName = row.app.packageName, contentDescription = row.app.label)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = row.app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                PermissionIcons(row, permissionNames)
            }
            if (row.risk.level != RiskLevel.NONE) {
                Spacer(Modifier.width(8.dp))
                RiskBadge(row.risk.level)
            }
        }
        HorizontalDivider(Modifier.padding(start = 72.dp))
    }
}

@Composable
private fun PermissionIcons(row: RatedApp, permissionNames: List<String>) {
    if (row.risk.grants.isEmpty()) {
        Text(
            text = stringResource(R.string.apps_no_sensitive_permissions),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        row.risk.grants.take(MAX_PERMISSION_ICONS).forEachIndexed { index, grant ->
            Icon(
                imageVector = grant.group.icon,
                contentDescription = permissionNames[index],
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        val more = row.risk.grants.size - MAX_PERMISSION_ICONS
        if (more > 0) {
            Text(
                text = stringResource(R.string.apps_more_permissions, more),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoAppsState(narrowed: Boolean, onClearFilters: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = Icons.Outlined.SearchOff,
            iconDescription = stringResource(R.string.apps_search_icon),
            title = stringResource(if (narrowed) R.string.apps_no_match_title else R.string.apps_none_title),
            message = stringResource(if (narrowed) R.string.apps_no_match_message else R.string.apps_none_message),
        )
        if (narrowed) {
            Spacer(Modifier.size(16.dp))
            Button(onClick = onClearFilters) { Text(stringResource(R.string.apps_clear_filters)) }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.apps_loading),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyState(
            icon = Icons.Outlined.ErrorOutline,
            iconDescription = stringResource(R.string.apps_error_icon),
            title = stringResource(R.string.apps_error_title),
            message = stringResource(R.string.apps_error_message),
        )
        Spacer(Modifier.size(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.apps_retry)) }
    }
}

@StringRes
private fun AppFilter.labelRes(): Int = when (this) {
    AppFilter.ALL -> R.string.apps_filter_all
    AppFilter.HIGH_RISK -> R.string.apps_filter_high_risk
    AppFilter.CAMERA -> R.string.apps_filter_camera
    AppFilter.MICROPHONE -> R.string.apps_filter_microphone
    AppFilter.LOCATION -> R.string.apps_filter_location
    AppFilter.CONTACTS -> R.string.apps_filter_contacts
}

@StringRes
private fun AppSort.labelRes(): Int = when (this) {
    AppSort.RISK -> R.string.apps_sort_risk
    AppSort.NAME -> R.string.apps_sort_name
    AppSort.RECENTLY_INSTALLED -> R.string.apps_sort_recent
}

private const val MAX_PERMISSION_ICONS = 5
