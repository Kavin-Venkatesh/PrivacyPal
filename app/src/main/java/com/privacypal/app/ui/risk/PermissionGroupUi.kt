package com.privacypal.app.ui.risk

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.ui.graphics.vector.ImageVector
import com.privacypal.app.R
import com.privacypal.app.domain.model.PermissionGroup

@StringRes
fun PermissionGroup.labelRes(): Int = when (this) {
    PermissionGroup.CAMERA -> R.string.permission_group_camera
    PermissionGroup.MICROPHONE -> R.string.permission_group_microphone
    PermissionGroup.LOCATION -> R.string.permission_group_location
    PermissionGroup.BACKGROUND_LOCATION -> R.string.permission_group_background_location
    PermissionGroup.CONTACTS -> R.string.permission_group_contacts
    PermissionGroup.SMS -> R.string.permission_group_sms
    PermissionGroup.CALL_LOGS -> R.string.permission_group_call_logs
    PermissionGroup.PHONE -> R.string.permission_group_phone
    PermissionGroup.BODY_SENSORS -> R.string.permission_group_body_sensors
    PermissionGroup.PHYSICAL_ACTIVITY -> R.string.permission_group_physical_activity
    PermissionGroup.NEARBY_DEVICES -> R.string.permission_group_nearby_devices
    PermissionGroup.MEDIA_FILES -> R.string.permission_group_media_files
    PermissionGroup.CALENDAR -> R.string.permission_group_calendar
    PermissionGroup.NOTIFICATIONS -> R.string.permission_group_notifications
}

val PermissionGroup.icon: ImageVector
    get() = when (this) {
        PermissionGroup.CAMERA -> Icons.Outlined.CameraAlt
        PermissionGroup.MICROPHONE -> Icons.Outlined.Mic
        PermissionGroup.LOCATION -> Icons.Outlined.LocationOn
        PermissionGroup.BACKGROUND_LOCATION -> Icons.Outlined.MyLocation
        PermissionGroup.CONTACTS -> Icons.Outlined.Contacts
        PermissionGroup.SMS -> Icons.Outlined.Sms
        PermissionGroup.CALL_LOGS -> Icons.Outlined.History
        PermissionGroup.PHONE -> Icons.Outlined.Call
        PermissionGroup.BODY_SENSORS -> Icons.Outlined.MonitorHeart
        PermissionGroup.PHYSICAL_ACTIVITY -> Icons.AutoMirrored.Outlined.DirectionsRun
        PermissionGroup.NEARBY_DEVICES -> Icons.Outlined.Bluetooth
        PermissionGroup.MEDIA_FILES -> Icons.Outlined.PhotoLibrary
        PermissionGroup.CALENDAR -> Icons.Outlined.CalendarMonth
        PermissionGroup.NOTIFICATIONS -> Icons.Outlined.Notifications
    }
