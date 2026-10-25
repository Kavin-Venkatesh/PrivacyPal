package com.privacypal.app

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun string(@StringRes id: Int) = composeRule.activity.getString(id)

    /** The bottom-bar tab, not the screen title that shares its text. */
    private fun tab(@StringRes labelRes: Int): SemanticsNodeInteraction = composeRule.onNode(
        hasText(string(labelRes)) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
    )

    @Test
    fun eachTabShowsItsScreen() {
        tab(R.string.nav_home).assertIsSelected()
        composeRule.onNodeWithText(string(R.string.home_empty_title)).assertIsDisplayed()

        tab(R.string.nav_apps).performClick().assertIsSelected()
        composeRule.onNodeWithText(string(R.string.apps_empty_title)).assertIsDisplayed()

        tab(R.string.nav_activity).performClick().assertIsSelected()
        composeRule.onNodeWithText(string(R.string.activity_empty_title)).assertIsDisplayed()

        tab(R.string.nav_settings).performClick().assertIsSelected()
        composeRule.onNodeWithText(string(R.string.settings_section_appearance)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_on_device_title)).assertIsDisplayed()

        tab(R.string.nav_home).performClick().assertIsSelected()
        composeRule.onNodeWithText(string(R.string.home_empty_title)).assertIsDisplayed()
    }

    @Test
    fun backFromTabReturnsHome() {
        tab(R.string.nav_activity).performClick()
        composeRule.onNodeWithText(string(R.string.activity_empty_title)).assertIsDisplayed()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        tab(R.string.nav_home).assertIsSelected()
        composeRule.onNodeWithText(string(R.string.home_empty_title)).assertIsDisplayed()
    }
}
