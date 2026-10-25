package com.privacypal.app.ui.apps

import com.privacypal.app.domain.apps.AppFilter
import com.privacypal.app.domain.apps.AppSort

/** Callbacks of the apps list, grouped so the screen signature stays readable. */
data class AppsActions(
    val onSearchChange: (String) -> Unit = {},
    val onFilterChange: (AppFilter) -> Unit = {},
    val onSortChange: (AppSort) -> Unit = {},
    val onShowSystemChange: (Boolean) -> Unit = {},
    val onClearFilters: () -> Unit = {},
    val onRetry: () -> Unit = {},
    /** Null while there is no detail screen to open. */
    val onAppClick: ((String) -> Unit)? = null,
)
