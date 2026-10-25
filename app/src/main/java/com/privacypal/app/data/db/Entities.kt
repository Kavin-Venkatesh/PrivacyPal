package com.privacypal.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.privacypal.app.domain.model.AppCategory
import com.privacypal.app.domain.model.PermissionGroup

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
