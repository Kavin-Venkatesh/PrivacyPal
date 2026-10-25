package com.privacypal.app.ui.activity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.privacypal.app.R
import com.privacypal.app.ui.components.EmptyStateScreen

@Composable
fun ActivityScreen(modifier: Modifier = Modifier) {
    EmptyStateScreen(
        title = stringResource(R.string.activity_title),
        icon = Icons.Outlined.Timeline,
        iconDescription = stringResource(R.string.activity_empty_icon),
        emptyTitle = stringResource(R.string.activity_empty_title),
        emptyMessage = stringResource(R.string.activity_empty_message),
        modifier = modifier,
    )
}
