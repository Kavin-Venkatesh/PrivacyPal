package com.privacypal.app.ui.risk

import androidx.annotation.StringRes
import com.privacypal.app.R
import com.privacypal.app.domain.risk.RiskLevel
import com.privacypal.app.domain.risk.RiskReason

@StringRes
fun RiskLevel.labelRes(): Int = when (this) {
    RiskLevel.NONE -> R.string.risk_level_none
    RiskLevel.LOW -> R.string.risk_level_low
    RiskLevel.MEDIUM -> R.string.risk_level_medium
    RiskLevel.HIGH -> R.string.risk_level_high
}

@StringRes
fun RiskReason.textRes(): Int = when (this) {
    RiskReason.EXPECTED_FOR_APP_TYPE -> R.string.risk_reason_expected
    RiskReason.UNDECLARED_PURPOSE -> R.string.risk_reason_undeclared_purpose
    RiskReason.SENSITIVE_CHECK_NEEDED -> R.string.risk_reason_sensitive_check
    RiskReason.MICROPHONE_UNEXPECTED -> R.string.risk_reason_microphone_unexpected
    RiskReason.CAMERA_UNEXPECTED -> R.string.risk_reason_camera_unexpected
    RiskReason.CONTACTS_UNEXPECTED -> R.string.risk_reason_contacts_unexpected
    RiskReason.LOCATION_UNEXPECTED -> R.string.risk_reason_location_unexpected
    RiskReason.PHONE_UNEXPECTED -> R.string.risk_reason_phone_unexpected
    RiskReason.BODY_SENSORS_UNEXPECTED -> R.string.risk_reason_body_sensors_unexpected
    RiskReason.PHYSICAL_ACTIVITY_UNEXPECTED -> R.string.risk_reason_physical_activity_unexpected
    RiskReason.CALENDAR_UNEXPECTED -> R.string.risk_reason_calendar_unexpected
    RiskReason.BACKGROUND_LOCATION -> R.string.risk_reason_background_location
    RiskReason.READS_MESSAGES -> R.string.risk_reason_reads_messages
    RiskReason.READS_CALL_HISTORY -> R.string.risk_reason_reads_call_history
    RiskReason.MEDIA_ACCESS -> R.string.risk_reason_media_access
    RiskReason.NEARBY_DEVICES_ACCESS -> R.string.risk_reason_nearby_devices
    RiskReason.SHOWS_NOTIFICATIONS -> R.string.risk_reason_notifications
}
