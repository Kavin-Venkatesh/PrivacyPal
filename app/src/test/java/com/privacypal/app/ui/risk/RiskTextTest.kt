package com.privacypal.app.ui.risk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.privacypal.app.domain.risk.RiskLevel
import com.privacypal.app.domain.risk.RiskReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RiskTextTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun everyReasonHasADistinctNonBlankString() {
        val texts = RiskReason.entries.map { context.getString(it.textRes()) }

        assertTrue(texts.all { it.isNotBlank() })
        assertEquals(texts.size, texts.toSet().size)
    }

    @Test
    fun everyLevelHasADistinctNonBlankLabel() {
        val labels = RiskLevel.entries.map { context.getString(it.labelRes()) }

        assertTrue(labels.all { it.isNotBlank() })
        assertEquals(labels.size, labels.toSet().size)
    }

    @Test
    fun apostrophesAreRenderedNotEscaped() {
        assertEquals(
            "This app doesn't say what it's for, so check whether it really needs this.",
            context.getString(RiskReason.UNDECLARED_PURPOSE.textRes()),
        )
    }
}
