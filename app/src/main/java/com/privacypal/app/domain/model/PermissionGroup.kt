package com.privacypal.app.domain.model

/** Sensitive permission groups PrivacyPal tracks. Background location is separate from foreground location. */
enum class PermissionGroup {
    CAMERA,
    MICROPHONE,
    LOCATION,
    BACKGROUND_LOCATION,
    CONTACTS,
    SMS,
    CALL_LOGS,
    PHONE,
    BODY_SENSORS,
    PHYSICAL_ACTIVITY,
    NEARBY_DEVICES,
    MEDIA_FILES,
    CALENDAR,
    NOTIFICATIONS,
}
