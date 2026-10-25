package com.privacypal.app.ui.apps.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** System "App info" page for [packageName], where the user can change its permissions. */
fun managePermissionsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))

/** Opens App info for [packageName]; falls back to the main Settings screen if that page is missing. */
fun Context.openManagePermissions(packageName: String) {
    try {
        startActivity(managePermissionsIntent(packageName))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
