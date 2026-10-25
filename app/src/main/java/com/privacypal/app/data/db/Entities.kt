package com.privacypal.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.privacypal.app.domain.model.AppCategory
import com.privacypal.app.domain.model.PermissionGroup
import com.privacypal.app.domain.watchdog.Confidence
import com.privacypal.app.domain.watchdog.Sensor

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val category: AppCategory,
    val isSystem: Boolean,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    /** When PrivacyPal last read this app from the system (epoch millis). */
    val scannedAt: Long,
)

/** One tracked permission an app requests; [granted] is its state at [AppEntity.scannedAt]. */
@Entity(
    tableName = "permission_grants",
    primaryKeys = ["packageName", "permission"],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("permissionGroup")],
)
data class PermissionGrantEntity(
    val packageName: String,
    val permission: String,
    val permissionGroup: PermissionGroup,
    val granted: Boolean,
)

data class AppWithGrants(
    @Embedded val app: AppEntity,
    @Relation(parentColumn = "packageName", entityColumn = "packageName")
    val grants: List<PermissionGrantEntity>,
)

/**
 * One period of microphone or camera use. [likelyPackage] is a guess (see LikelyAppRules), filled
 * in a few seconds after the start, once [likelyResolved].
 */
@Entity(tableName = "sensor_events", indices = [Index("startedAt"), Index("likelyPackage")])
data class SensorEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sensor: Sensor,
    val startedAt: Long,
    /** null while still in use, or if PrivacyPal stopped before the end was seen ([interrupted]). */
    val endedAt: Long? = null,
    /** PrivacyPal was stopped (e.g. killed) while this was in use, so the end time is unknown. */
    val interrupted: Boolean = false,
    val screenOn: Boolean,
    val likelyResolved: Boolean = false,
    val likelyPackage: String? = null,
    val confidence: Confidence? = null,
    /** Whether an app was on screen at the start; null when unknown (no usage access). */
    val appInForeground: Boolean? = null,
)

/** Per-app watchdog choices, e.g. not alerting for an app the user trusts. */
@Entity(tableName = "app_rules")
data class AppRuleEntity(@PrimaryKey val packageName: String, val muteAlerts: Boolean)
