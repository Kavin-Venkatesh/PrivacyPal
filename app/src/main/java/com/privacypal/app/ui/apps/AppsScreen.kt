package com.privacypal.app.ui.apps

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.privacypal.app.R
import com.privacypal.app.ui.components.EmptyStateScreen

@Composable
fun AppsScreen(modifier: Modifier = Modifier) {
    EmptyStateScreen(
        title = stringResource(R.string.apps_title),
        icon = Icons.Outlined.Apps,
        iconDescription = stringResource(R.string.apps_empty_icon),
        emptyTitle = stringResource(R.string.apps_empty_title),
        emptyMessage = stringResource(R.string.apps_empty_message),
        modifier = modifier,
    )
}
