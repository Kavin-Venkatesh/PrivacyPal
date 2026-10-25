package com.privacypal.app.ui.apps

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.privacypal.app.R
import com.privacypal.app.domain.apps.AppFilter
import com.privacypal.app.testing.FakeAppsRepository
import com.privacypal.app.testing.sampleApps
import com.privacypal.app.ui.theme.PrivacyPalTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The real screen and ViewModel against a fake repository, run on the JVM (and in CI). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2000dp")
class AppsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val viewModel = AppsViewModel(FakeAppsRepository(sampleApps), SavedStateHandle())
    private var clicked: String? = null

    private fun string(id: Int, vararg args: Any) = context.getString(id, *args)

    /** Row labels in on-screen order, read from each row's merged description. */
    private fun rowLabels(): List<String> {
        val names = sampleApps.map { it.label }
        return composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                node.config[SemanticsProperties.ContentDescription].first().substringBefore(". ").takeIf { it in names }
            }
    }

    @Before
    fun setUp() {
        composeRule.setContent {
            PrivacyPalTheme(darkTheme = false, dynamicColor = false) {
                AppsRoute(viewModel = viewModel, onAppClick = { clicked = it })
            }
        }
    }

    @Test
    fun showsVisibleAppsSortedByRisk() {
        assertEquals(listOf("Bright Torch", "Weather Now", "Chatter", "Notes", "Calculator"), rowLabels())
    }

    @Test
    fun searchFiltersRows() {
        composeRule.onNode(hasSetTextAction()).performTextInput("weather")

        assertEquals(listOf("Weather Now"), rowLabels())
    }

    @Test
    fun noMatchShowsClearFiltersWhichRestoresTheList() {
        composeRule.onNode(hasSetTextAction()).performTextInput("zzz")
        composeRule.onNodeWithText(string(R.string.apps_no_match_title)).assertExists()

        composeRule.onNodeWithText(string(R.string.apps_clear_filters)).performClick()

        assertEquals(5, rowLabels().size)
    }

    @Test
    fun chipFiltersRows() {
        composeRule.onNodeWithText(string(R.string.apps_filter_camera)).performClick()

        assertEquals(listOf("Bright Torch", "Chatter"), rowLabels())
    }

    @Test
    fun sortMenuChangesOrder() {
        composeRule.onNodeWithText(string(R.string.apps_sort_current, string(R.string.apps_sort_risk))).performClick()
        composeRule.onNodeWithText(string(R.string.apps_sort_name)).performClick()

        assertEquals(listOf("Bright Torch", "Calculator", "Chatter", "Notes", "Weather Now"), rowLabels())
    }

    @Test
    fun systemToggleShowsSystemApps() {
        composeRule.onNodeWithText(string(R.string.apps_show_system)).performClick()

        assertEquals(6, rowLabels().size)
    }

    @Test
    fun tappingSummaryLineAppliesItsFilter() {
        val highRisk = context.resources.getQuantityString(R.plurals.apps_summary_high_risk, 2, 2)

        composeRule.onNodeWithText(highRisk).performClick()

        assertEquals(listOf("Bright Torch", "Weather Now"), rowLabels())
        assertEquals(AppFilter.HIGH_RISK, viewModel.query.value.filter)
    }

    @Test
    fun rowsAnnounceNameRiskAndPermissionsAndOpenDetail() {
        val description = "Weather Now. ${string(R.string.risk_level_high)}. " +
            string(R.string.apps_row_permissions, "Location (all the time), Location, Notifications")
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription(description))
        composeRule.onAllNodesWithContentDescription(description).assertCountEquals(1)

        composeRule.onAllNodesWithContentDescription(description)[0].performClick()

        assertEquals("com.example.weather", clicked)
    }
}
