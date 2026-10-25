package com.privacypal.app.domain.risk

/**
 * Plain-language explanation behind a [RiskLevel]. The UI maps each value to a string resource,
 * so the domain stays free of Android types.
 */
enum class RiskReason {
    /** The app's declared type normally needs this permission. */
    EXPECTED_FOR_APP_TYPE,

    /** The app declares no type, so PrivacyPal can't tell whether it needs this. */
    UNDECLARED_PURPOSE,

    /** Sensitive, and the app's type neither clearly needs nor clearly rules it out. */
    SENSITIVE_CHECK_NEEDED,

    MICROPHONE_UNEXPECTED,
    CAMERA_UNEXPECTED,
    CONTACTS_UNEXPECTED,
    LOCATION_UNEXPECTED,
    PHONE_UNEXPECTED,
    BODY_SENSORS_UNEXPECTED,
    PHYSICAL_ACTIVITY_UNEXPECTED,
    CALENDAR_UNEXPECTED,

    /** Location "all the time": the app can track you while you're not using it. */
    BACKGROUND_LOCATION,

    /** Can read text messages, including one-time login codes. */
    READS_MESSAGES,

    /** Can see who you call and when. */
    READS_CALL_HISTORY,

    MEDIA_ACCESS,
    NEARBY_DEVICES_ACCESS,
    SHOWS_NOTIFICATIONS,
}
