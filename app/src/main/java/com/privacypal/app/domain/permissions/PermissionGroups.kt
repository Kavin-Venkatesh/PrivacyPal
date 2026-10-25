package com.privacypal.app.domain.permissions

import com.privacypal.app.domain.model.PermissionGroup

/**
 * Maps raw Android permission names to the groups PrivacyPal tracks. Anything not listed
 * (normal or signature permissions) is not tracked.
 */
object PermissionGroups {

    private const val P = "android.permission."

    private val byPermission: Map<String, PermissionGroup> = buildMap {
        put("${P}CAMERA", PermissionGroup.CAMERA)

        put("${P}RECORD_AUDIO", PermissionGroup.MICROPHONE)

        put("${P}ACCESS_FINE_LOCATION", PermissionGroup.LOCATION)
        put("${P}ACCESS_COARSE_LOCATION", PermissionGroup.LOCATION)
        put("${P}ACCESS_BACKGROUND_LOCATION", PermissionGroup.BACKGROUND_LOCATION)

        put("${P}READ_CONTACTS", PermissionGroup.CONTACTS)
        put("${P}WRITE_CONTACTS", PermissionGroup.CONTACTS)
        put("${P}GET_ACCOUNTS", PermissionGroup.CONTACTS)

        put("${P}SEND_SMS", PermissionGroup.SMS)
        put("${P}RECEIVE_SMS", PermissionGroup.SMS)
        put("${P}READ_SMS", PermissionGroup.SMS)
        put("${P}RECEIVE_WAP_PUSH", PermissionGroup.SMS)
        put("${P}RECEIVE_MMS", PermissionGroup.SMS)

        put("${P}READ_CALL_LOG", PermissionGroup.CALL_LOGS)
        put("${P}WRITE_CALL_LOG", PermissionGroup.CALL_LOGS)
        put("${P}PROCESS_OUTGOING_CALLS", PermissionGroup.CALL_LOGS)

        put("${P}READ_PHONE_STATE", PermissionGroup.PHONE)
        put("${P}READ_PHONE_NUMBERS", PermissionGroup.PHONE)
        put("${P}CALL_PHONE", PermissionGroup.PHONE)
        put("${P}ANSWER_PHONE_CALLS", PermissionGroup.PHONE)
        put("com.android.voicemail.permission.ADD_VOICEMAIL", PermissionGroup.PHONE)
        put("${P}USE_SIP", PermissionGroup.PHONE)
        put("${P}ACCEPT_HANDOVER", PermissionGroup.PHONE)

        put("${P}BODY_SENSORS", PermissionGroup.BODY_SENSORS)
        put("${P}BODY_SENSORS_BACKGROUND", PermissionGroup.BODY_SENSORS)
        // Android 16 replaces BODY_SENSORS with health permissions.
        put("${P}health.READ_HEART_RATE", PermissionGroup.BODY_SENSORS)
        put("${P}health.READ_HEALTH_DATA_IN_BACKGROUND", PermissionGroup.BODY_SENSORS)

        put("${P}ACTIVITY_RECOGNITION", PermissionGroup.PHYSICAL_ACTIVITY)

        put("${P}BLUETOOTH_SCAN", PermissionGroup.NEARBY_DEVICES)
        put("${P}BLUETOOTH_CONNECT", PermissionGroup.NEARBY_DEVICES)
        put("${P}BLUETOOTH_ADVERTISE", PermissionGroup.NEARBY_DEVICES)
        put("${P}NEARBY_WIFI_DEVICES", PermissionGroup.NEARBY_DEVICES)
        put("${P}UWB_RANGING", PermissionGroup.NEARBY_DEVICES)

        put("${P}READ_MEDIA_IMAGES", PermissionGroup.MEDIA_FILES)
        put("${P}READ_MEDIA_VIDEO", PermissionGroup.MEDIA_FILES)
        put("${P}READ_MEDIA_AUDIO", PermissionGroup.MEDIA_FILES)
        put("${P}READ_MEDIA_VISUAL_USER_SELECTED", PermissionGroup.MEDIA_FILES)
        put("${P}READ_EXTERNAL_STORAGE", PermissionGroup.MEDIA_FILES)
        put("${P}WRITE_EXTERNAL_STORAGE", PermissionGroup.MEDIA_FILES)
        put("${P}ACCESS_MEDIA_LOCATION", PermissionGroup.MEDIA_FILES)

        put("${P}READ_CALENDAR", PermissionGroup.CALENDAR)
        put("${P}WRITE_CALENDAR", PermissionGroup.CALENDAR)

        put("${P}POST_NOTIFICATIONS", PermissionGroup.NOTIFICATIONS)
    }

    /** All tracked permission names. */
    val trackedPermissions: Set<String> get() = byPermission.keys

    /** The group for [permission], or null if PrivacyPal does not track it. */
    fun groupOf(permission: String): PermissionGroup? = byPermission[permission]
}
