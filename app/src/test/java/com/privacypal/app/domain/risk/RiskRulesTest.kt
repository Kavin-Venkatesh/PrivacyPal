package com.privacypal.app.domain.risk

import com.privacypal.app.domain.model.AppCategory
import com.privacypal.app.domain.model.AppCategory.ACCESSIBILITY
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
import com.privacypal.app.domain.model.PermissionGroup.BACKGROUND_LOCATION
import com.privacypal.app.domain.model.PermissionGroup.BODY_SENSORS
import com.privacypal.app.domain.model.PermissionGroup.CALENDAR
import com.privacypal.app.domain.model.PermissionGroup.CALL_LOGS
import com.privacypal.app.domain.model.PermissionGroup.CAMERA
import com.privacypal.app.domain.model.PermissionGroup.CONTACTS
import com.privacypal.app.domain.model.PermissionGroup.LOCATION
import com.privacypal.app.domain.model.PermissionGroup.MEDIA_FILES
import com.privacypal.app.domain.model.PermissionGroup.MICROPHONE
import com.privacypal.app.domain.model.PermissionGroup.NEARBY_DEVICES
import com.privacypal.app.domain.model.PermissionGroup.NOTIFICATIONS
import com.privacypal.app.domain.model.PermissionGroup.PHONE
import com.privacypal.app.domain.model.PermissionGroup.PHYSICAL_ACTIVITY
import com.privacypal.app.domain.model.PermissionGroup.SMS
import com.privacypal.app.domain.risk.RiskLevel.HIGH
import com.privacypal.app.domain.risk.RiskLevel.LOW
import com.privacypal.app.domain.risk.RiskLevel.MEDIUM
import org.junit.Assert.assertEquals
import org.junit.Test

class RiskRulesTest {

    /** The approved rule table (docs/PLAN.md), written independently of [RiskRules]. */
    private fun expectedLevel(group: PermissionGroup, category: AppCategory): RiskLevel = when (group) {
        MICROPHONE -> table(
            category,
            low = setOf(SOCIAL, VIDEO, AUDIO),
            high = setOf(GAME, NEWS, MAPS, IMAGE, PRODUCTIVITY),
        )

        CAMERA -> table(category, low = setOf(SOCIAL, VIDEO, IMAGE), high = setOf(GAME, NEWS, AUDIO, MAPS))

        CONTACTS -> table(
            category,
            low = setOf(SOCIAL),
            high = setOf(GAME, NEWS, PRODUCTIVITY, IMAGE, AUDIO, VIDEO, MAPS),
        )

        LOCATION -> table(category, low = setOf(MAPS), high = setOf(GAME, AUDIO))

        BACKGROUND_LOCATION -> if (category == MAPS) MEDIUM else HIGH

        SMS, CALL_LOGS -> if (category == SOCIAL) MEDIUM else HIGH

        PHONE -> table(category, low = setOf(SOCIAL), high = setOf(GAME, NEWS))

        BODY_SENSORS -> table(category, low = emptySet(), high = setOf(GAME, NEWS, SOCIAL))

        PHYSICAL_ACTIVITY -> table(category, low = emptySet(), high = setOf(GAME))

        CALENDAR -> table(category, low = setOf(PRODUCTIVITY), high = setOf(GAME))

        NEARBY_DEVICES, MEDIA_FILES, NOTIFICATIONS -> LOW
    }

    private fun table(category: AppCategory, low: Set<AppCategory>, high: Set<AppCategory>) = when (category) {
        in low -> LOW
        in high -> HIGH
        else -> MEDIUM
    }

    @Test
    fun everyGroupAndCategoryFollowsTheApprovedTable() {
        val mismatches = PermissionGroup.entries.flatMap { group ->
            AppCategory.entries.mapNotNull { category ->
                val actual = RiskRules.assess(group, category).level
                val expected = expectedLevel(group, category)
                "$group/$category: expected $expected, got $actual".takeIf { actual != expected }
            }
        }
        assertEquals(emptyList<String>(), mismatches)
    }

    @Test
    fun gameWithMicrophoneIsHighWithSpecificReason() {
        assertEquals(
            GrantRisk(MICROPHONE, HIGH, RiskReason.MICROPHONE_UNEXPECTED),
            RiskRules.assess(MICROPHONE, GAME),
        )
    }

    @Test
    fun expectedPermissionIsLowAndSaysSo() {
        assertEquals(GrantRisk(CAMERA, LOW, RiskReason.EXPECTED_FOR_APP_TYPE), RiskRules.assess(CAMERA, SOCIAL))
    }

    @Test
    fun undefinedCategoryIsMediumWithHonestReason() {
        listOf(CAMERA, MICROPHONE, LOCATION, CONTACTS, PHONE, BODY_SENSORS, PHYSICAL_ACTIVITY, CALENDAR).forEach {
            assertEquals(GrantRisk(it, MEDIUM, RiskReason.UNDECLARED_PURPOSE), RiskRules.assess(it, UNDEFINED))
        }
    }

    @Test
    fun declaredCategoryWithoutRuleIsMediumCheckNeeded() {
        assertEquals(
            GrantRisk(MICROPHONE, MEDIUM, RiskReason.SENSITIVE_CHECK_NEEDED),
            RiskRules.assess(MICROPHONE, ACCESSIBILITY),
        )
    }

    @Test
    fun inherentlyRiskyGroupsKeepTheirReasonForUndefinedApps() {
        assertEquals(GrantRisk(SMS, HIGH, RiskReason.READS_MESSAGES), RiskRules.assess(SMS, UNDEFINED))
        assertEquals(GrantRisk(CALL_LOGS, HIGH, RiskReason.READS_CALL_HISTORY), RiskRules.assess(CALL_LOGS, UNDEFINED))
        assertEquals(
            GrantRisk(BACKGROUND_LOCATION, HIGH, RiskReason.BACKGROUND_LOCATION),
            RiskRules.assess(BACKGROUND_LOCATION, UNDEFINED),
        )
        assertEquals(
            GrantRisk(BACKGROUND_LOCATION, MEDIUM, RiskReason.BACKGROUND_LOCATION),
            RiskRules.assess(BACKGROUND_LOCATION, MAPS),
        )
    }

    @Test
    fun lowGroupsHaveDescriptiveReasons() {
        assertEquals(RiskReason.MEDIA_ACCESS, RiskRules.assess(MEDIA_FILES, UNDEFINED).reason)
        assertEquals(RiskReason.NEARBY_DEVICES_ACCESS, RiskRules.assess(NEARBY_DEVICES, GAME).reason)
        assertEquals(RiskReason.SHOWS_NOTIFICATIONS, RiskRules.assess(NOTIFICATIONS, NEWS).reason)
    }

    @Test
    fun everyReasonIsReachable() {
        val produced = PermissionGroup.entries.flatMap { group ->
            AppCategory.entries.map { RiskRules.assess(group, it).reason }
        }.toSet()
        assertEquals(RiskReason.entries.toSet(), produced)
    }
}
