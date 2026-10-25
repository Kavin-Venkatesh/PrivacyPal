package com.privacypal.app.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.privacypal.app.R
import com.privacypal.app.ui.components.EmptyStateScreen

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    EmptyStateScreen(
        title = stringResource(R.string.home_title),
        icon = Icons.Outlined.Shield,
        iconDescription = stringResource(R.string.home_empty_icon),
        emptyTitle = stringResource(R.string.home_empty_title),
        emptyMessage = stringResource(R.string.home_empty_message),
        modifier = modifier,
    )
}
