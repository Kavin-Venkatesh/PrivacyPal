package com.privacypal.app.domain.permissions

import com.privacypal.app.domain.model.PermissionGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionGroupsTest {

    @Test
    fun mapsRepresentativePermissionOfEveryGroup() {
        val expected = mapOf(
            "android.permission.CAMERA" to PermissionGroup.CAMERA,
            "android.permission.RECORD_AUDIO" to PermissionGroup.MICROPHONE,
            "android.permission.ACCESS_FINE_LOCATION" to PermissionGroup.LOCATION,
            "android.permission.ACCESS_COARSE_LOCATION" to PermissionGroup.LOCATION,
            "android.permission.ACCESS_BACKGROUND_LOCATION" to PermissionGroup.BACKGROUND_LOCATION,
            "android.permission.READ_CONTACTS" to PermissionGroup.CONTACTS,
            "android.permission.READ_SMS" to PermissionGroup.SMS,
            "android.permission.READ_CALL_LOG" to PermissionGroup.CALL_LOGS,
            "android.permission.READ_PHONE_STATE" to PermissionGroup.PHONE,
            "com.android.voicemail.permission.ADD_VOICEMAIL" to PermissionGroup.PHONE,
            "android.permission.BODY_SENSORS" to PermissionGroup.BODY_SENSORS,
            "android.permission.health.READ_HEART_RATE" to PermissionGroup.BODY_SENSORS,
            "android.permission.ACTIVITY_RECOGNITION" to PermissionGroup.PHYSICAL_ACTIVITY,
            "android.permission.BLUETOOTH_CONNECT" to PermissionGroup.NEARBY_DEVICES,
            "android.permission.NEARBY_WIFI_DEVICES" to PermissionGroup.NEARBY_DEVICES,
            "android.permission.READ_MEDIA_IMAGES" to PermissionGroup.MEDIA_FILES,
            "android.permission.READ_EXTERNAL_STORAGE" to PermissionGroup.MEDIA_FILES,
            "android.permission.READ_CALENDAR" to PermissionGroup.CALENDAR,
            "android.permission.POST_NOTIFICATIONS" to PermissionGroup.NOTIFICATIONS,
        )
        expected.forEach { (permission, group) ->
            assertEquals(permission, group, PermissionGroups.groupOf(permission))
        }
    }

    @Test
    fun everyGroupHasAtLeastOnePermission() {
        val covered = PermissionGroups.trackedPermissions.mapNotNull(PermissionGroups::groupOf).toSet()
        assertEquals(PermissionGroup.entries.toSet(), covered)
    }

    @Test
    fun normalAndUnknownPermissionsAreNotTracked() {
        assertNull(PermissionGroups.groupOf("android.permission.INTERNET"))
        assertNull(PermissionGroups.groupOf("android.permission.VIBRATE"))
        assertNull(PermissionGroups.groupOf("com.example.CUSTOM_PERMISSION"))
        assertNull(PermissionGroups.groupOf(""))
    }

    @Test
    fun lookupIsCaseSensitive() {
        assertNull(PermissionGroups.groupOf("android.permission.camera"))
        assertTrue("android.permission.CAMERA" in PermissionGroups.trackedPermissions)
    }
}
