package com.privacypal.app.domain.risk

import com.privacypal.app.domain.model.AppCategory
import com.privacypal.app.domain.model.PermissionGroup
import com.privacypal.app.testing.BACKGROUND_LOCATION
import com.privacypal.app.testing.CAMERA
import com.privacypal.app.testing.FINE_LOCATION
import com.privacypal.app.testing.POST_NOTIFICATIONS
import com.privacypal.app.testing.READ_SMS
import com.privacypal.app.testing.RECORD_AUDIO
import com.privacypal.app.testing.testApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRiskTest {

    @Test
    fun appWithoutPermissionsHasNoRisk() {
        val risk = assessApp(testApp())

        assertEquals(RiskLevel.NONE, risk.level)
        assertEquals(0, risk.flaggedCount)
        assertTrue(risk.grants.isEmpty())
    }

    @Test
    fun requestedButDeniedPermissionsAreIgnored() {
        val risk = assessApp(testApp(category = AppCategory.GAME, requestedOnly = listOf(RECORD_AUDIO, READ_SMS)))

        assertEquals(RiskLevel.NONE, risk.level)
        assertTrue(risk.grants.isEmpty())
    }

    @Test
    fun levelIsTheWorstGrantAndGrantsAreSortedWorstFirst() {
        val app = testApp(
            category = AppCategory.SOCIAL,
            granted = listOf(POST_NOTIFICATIONS, CAMERA, READ_SMS, FINE_LOCATION),
        )

        val risk = assessApp(app)

        assertEquals(RiskLevel.MEDIUM, risk.level)
        assertEquals(
            listOf(
                PermissionGroup.LOCATION,
                PermissionGroup.SMS,
                PermissionGroup.CAMERA,
                PermissionGroup.NOTIFICATIONS,
            ),
            risk.grants.map { it.group },
        )
        assertEquals(2, risk.flaggedCount)
    }

    @Test
    fun onlyLowGrantsMakeALowRiskApp() {
        val risk = assessApp(testApp(category = AppCategory.SOCIAL, granted = listOf(CAMERA, POST_NOTIFICATIONS)))

        assertEquals(RiskLevel.LOW, risk.level)
        assertEquals(0, risk.flaggedCount)
    }

    @Test
    fun oneHighGrantMakesTheAppHigh() {
        val app = testApp(granted = listOf(POST_NOTIFICATIONS, FINE_LOCATION, BACKGROUND_LOCATION))

        val risk = assessApp(app)

        assertEquals(RiskLevel.HIGH, risk.level)
        assertEquals(PermissionGroup.BACKGROUND_LOCATION, risk.grants.first().group)
        assertEquals(2, risk.flaggedCount)
    }

    @Test
    fun severalPermissionsInOneGroupCountOnce() {
        val app = testApp(granted = listOf(FINE_LOCATION, "android.permission.ACCESS_COARSE_LOCATION"))

        assertEquals(1, assessApp(app).grants.size)
    }
}
