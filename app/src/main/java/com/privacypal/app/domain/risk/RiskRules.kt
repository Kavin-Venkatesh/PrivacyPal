package com.privacypal.app.domain.risk

import com.privacypal.app.domain.model.AppCategory
import com.privacypal.app.domain.model.AppCategory.AUDIO
import com.privacypal.app.domain.model.AppCategory.GAME
import com.privacypal.app.domain.model.AppCategory.IMAGE
import com.privacypal.app.domain.model.AppCategory.MAPS
import com.privacypal.app.domain.model.AppCategory.NEWS
import com.privacypal.app.domain.model.AppCategory.PRODUCTIVITY
import com.privacypal.app.domain.model.AppCategory.SOCIAL
import com.privacypal.app.domain.model.AppCategory.UNDEFINED
import com.privacypal.app.domain.model.AppCategory.VIDEO
import com.privacypal.app.domain.model.PermissionGroup

/** Overrides a group's default rating when the app declares one of [categories]. */
data class RiskRule(
    val group: PermissionGroup,
    val categories: Set<AppCategory>,
    val level: RiskLevel,
    val reason: RiskReason,
)

/** Rating of one granted permission group. */
data class GrantRisk(val group: PermissionGroup, val level: RiskLevel, val reason: RiskReason)

/**
 * Category-aware risk rules. A rule matching the app's category wins; otherwise the group's
 * [default][defaultFor] applies. Apps declaring no category get the default, with an honest reason
 * when that default is "check it" (MEDIUM).
 */
object RiskRules {

    val rules: List<RiskRule> = listOf(
        low(PermissionGroup.MICROPHONE, SOCIAL, VIDEO, AUDIO),
        high(PermissionGroup.MICROPHONE, RiskReason.MICROPHONE_UNEXPECTED, GAME, NEWS, MAPS, IMAGE, PRODUCTIVITY),
        low(PermissionGroup.CAMERA, SOCIAL, VIDEO, IMAGE),
        high(PermissionGroup.CAMERA, RiskReason.CAMERA_UNEXPECTED, GAME, NEWS, AUDIO, MAPS),
        low(PermissionGroup.CONTACTS, SOCIAL),
        high(
            PermissionGroup.CONTACTS,
            RiskReason.CONTACTS_UNEXPECTED,
            GAME,
            NEWS,
            PRODUCTIVITY,
            IMAGE,
            AUDIO,
            VIDEO,
            MAPS,
        ),
        low(PermissionGroup.LOCATION, MAPS),
        high(PermissionGroup.LOCATION, RiskReason.LOCATION_UNEXPECTED, GAME, AUDIO),
        RiskRule(PermissionGroup.BACKGROUND_LOCATION, setOf(MAPS), RiskLevel.MEDIUM, RiskReason.BACKGROUND_LOCATION),
        RiskRule(PermissionGroup.SMS, setOf(SOCIAL), RiskLevel.MEDIUM, RiskReason.READS_MESSAGES),
        RiskRule(PermissionGroup.CALL_LOGS, setOf(SOCIAL), RiskLevel.MEDIUM, RiskReason.READS_CALL_HISTORY),
        low(PermissionGroup.PHONE, SOCIAL),
        high(PermissionGroup.PHONE, RiskReason.PHONE_UNEXPECTED, GAME, NEWS),
        high(PermissionGroup.BODY_SENSORS, RiskReason.BODY_SENSORS_UNEXPECTED, GAME, NEWS, SOCIAL),
        high(PermissionGroup.PHYSICAL_ACTIVITY, RiskReason.PHYSICAL_ACTIVITY_UNEXPECTED, GAME),
        low(PermissionGroup.CALENDAR, PRODUCTIVITY),
        high(PermissionGroup.CALENDAR, RiskReason.CALENDAR_UNEXPECTED, GAME),
    )

    init {
        rules.groupBy { it.group }.forEach { (group, groupRules) ->
            val categories = groupRules.flatMap { it.categories }
            require(categories.size == categories.toSet().size) { "Overlapping rules for $group" }
            require(UNDEFINED !in categories) { "UNDEFINED is handled by the default for $group" }
        }
    }

    /** Rating used when no [rules] entry matches the app's category. */
    fun defaultFor(group: PermissionGroup): Pair<RiskLevel, RiskReason> = when (group) {
        PermissionGroup.CAMERA,
        PermissionGroup.MICROPHONE,
        PermissionGroup.LOCATION,
        PermissionGroup.CONTACTS,
        PermissionGroup.PHONE,
        PermissionGroup.BODY_SENSORS,
        PermissionGroup.PHYSICAL_ACTIVITY,
        PermissionGroup.CALENDAR,
        -> RiskLevel.MEDIUM to RiskReason.SENSITIVE_CHECK_NEEDED

        PermissionGroup.BACKGROUND_LOCATION -> RiskLevel.HIGH to RiskReason.BACKGROUND_LOCATION

        PermissionGroup.SMS -> RiskLevel.HIGH to RiskReason.READS_MESSAGES

        PermissionGroup.CALL_LOGS -> RiskLevel.HIGH to RiskReason.READS_CALL_HISTORY

        PermissionGroup.MEDIA_FILES -> RiskLevel.LOW to RiskReason.MEDIA_ACCESS

        PermissionGroup.NEARBY_DEVICES -> RiskLevel.LOW to RiskReason.NEARBY_DEVICES_ACCESS

        PermissionGroup.NOTIFICATIONS -> RiskLevel.LOW to RiskReason.SHOWS_NOTIFICATIONS
    }

    fun assess(group: PermissionGroup, category: AppCategory): GrantRisk {
        rules.firstOrNull { it.group == group && category in it.categories }?.let {
            return GrantRisk(group, it.level, it.reason)
        }
        val (level, reason) = defaultFor(group)
        val honestReason = if (category == UNDEFINED && level == RiskLevel.MEDIUM) {
            RiskReason.UNDECLARED_PURPOSE
        } else {
            reason
        }
        return GrantRisk(group, level, honestReason)
    }

    private fun low(group: PermissionGroup, vararg categories: AppCategory) =
        RiskRule(group, categories.toSet(), RiskLevel.LOW, RiskReason.EXPECTED_FOR_APP_TYPE)

    private fun high(group: PermissionGroup, reason: RiskReason, vararg categories: AppCategory) =
        RiskRule(group, categories.toSet(), RiskLevel.HIGH, reason)
}
