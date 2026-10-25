package com.privacypal.app.domain.apps

import com.privacypal.app.domain.model.InstalledApp

/** Apps to show: system apps are hidden unless [showSystem] is on. */
fun visibleApps(apps: List<InstalledApp>, showSystem: Boolean = false): List<InstalledApp> =
    if (showSystem) apps else apps.filterNot { it.isSystem }
