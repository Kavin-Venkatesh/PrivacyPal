package com.privacypal.app.domain.apps

import com.privacypal.app.testing.sampleApps
import com.privacypal.app.testing.testApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppQueryTest {

    private val rated = sampleApps.map { it.rated() }

    private fun labels(query: AppQuery) = rated.applyQuery(query).map { it.app.label }

    @Test
    fun defaultQueryHidesSystemAppsAndSortsByRisk() {
        assertEquals(
            listOf("Bright Torch", "Weather Now", "Chatter", "Notes", "Calculator"),
            labels(AppQuery()),
        )
    }

    @Test
    fun riskSortBreaksTiesByFlaggedCountThenName() {
        val apps = listOf(
            testApp(packageName = "b", label = "beta", granted = listOf(CAMERA_PERMISSION)),
            testApp(packageName = "a", label = "Alpha", granted = listOf(CAMERA_PERMISSION)),
            testApp(packageName = "c", label = "Gamma", granted = listOf(CAMERA_PERMISSION, MIC_PERMISSION)),
        ).map { it.rated() }

        assertEquals(listOf("Gamma", "Alpha", "beta"), apps.applyQuery(AppQuery()).map { it.app.label })
    }

    @Test
    fun showSystemIncludesSystemApps() {
        assertTrue("System Thing" in labels(AppQuery(showSystem = true)))
    }

    @Test
    fun searchMatchesLabelOrPackageIgnoringCaseAndSpaces() {
        assertEquals(listOf("Chatter"), labels(AppQuery(search = "  CHAT ")))
        assertEquals(listOf("Weather Now"), labels(AppQuery(search = "example.weather")))
        assertEquals(emptyList<String>(), labels(AppQuery(search = "nothing like this")))
    }

    @Test
    fun filtersMatchGrantedGroupsOnly() {
        assertEquals(listOf("Bright Torch", "Chatter"), labels(AppQuery(filter = AppFilter.CAMERA)))
        // Notes only requested the microphone; it is not granted.
        assertEquals(listOf("Bright Torch", "Chatter"), labels(AppQuery(filter = AppFilter.MICROPHONE)))
        assertEquals(listOf("Bright Torch", "Chatter"), labels(AppQuery(filter = AppFilter.CONTACTS)))
    }

    @Test
    fun locationFilterIncludesBackgroundLocation() {
        val onlyBackground = testApp(packageName = "bg", label = "Bg", granted = listOf(BG_LOCATION)).rated()

        val result = listOf(onlyBackground).applyQuery(AppQuery(filter = AppFilter.LOCATION))

        assertEquals(listOf("Bg"), result.map { it.app.label })
    }

    @Test
    fun highRiskFilterKeepsOnlyHighApps() {
        assertEquals(listOf("Bright Torch", "Weather Now"), labels(AppQuery(filter = AppFilter.HIGH_RISK)))
    }

    @Test
    fun nameSortIsCaseInsensitive() {
        assertEquals(
            listOf("Bright Torch", "Calculator", "Chatter", "Notes", "Weather Now"),
            labels(AppQuery(sort = AppSort.NAME)),
        )
    }

    @Test
    fun recentlyInstalledSortsNewestFirst() {
        assertEquals(
            listOf("Bright Torch", "Chatter", "Weather Now", "Notes", "Calculator"),
            labels(AppQuery(sort = AppSort.RECENTLY_INSTALLED)),
        )
    }

    @Test
    fun searchAndFilterCombine() {
        assertEquals(listOf("Chatter"), labels(AppQuery(search = "chat", filter = AppFilter.CAMERA)))
        assertEquals(emptyList<String>(), labels(AppQuery(search = "notes", filter = AppFilter.CAMERA)))
    }

    @Test
    fun summaryCountsVisibleAppsIgnoringSearchAndFilter() {
        assertEquals(
            AppsSummary(total = 5, highRisk = 2, camera = 2, microphone = 2, location = 2, contacts = 2),
            summarize(rated, showSystem = false),
        )
        assertEquals(6, summarize(rated, showSystem = true).total)
        assertEquals(3, summarize(rated, showSystem = true).highRisk)
    }

    @Test
    fun summaryCountMatchesFilter() {
        val summary = summarize(rated, showSystem = false)

        AppFilter.entries.forEach { filter ->
            assertEquals(filter.name, labels(AppQuery(filter = filter)).size, summary.count(filter))
        }
    }

    @Test
    fun isNarrowedOnlyForSearchOrFilter() {
        assertFalse(AppQuery(sort = AppSort.NAME, showSystem = true).isNarrowed)
        assertFalse(AppQuery(search = "   ").isNarrowed)
        assertTrue(AppQuery(search = "a").isNarrowed)
        assertTrue(AppQuery(filter = AppFilter.CAMERA).isNarrowed)
    }

    private companion object {
        const val CAMERA_PERMISSION = "android.permission.CAMERA"
        const val MIC_PERMISSION = "android.permission.RECORD_AUDIO"
        const val BG_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"
    }
}
