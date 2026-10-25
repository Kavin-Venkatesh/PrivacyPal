package com.privacypal.app.domain.apps

import com.privacypal.app.testing.testApp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppListFilterTest {

    private val user = testApp(packageName = "com.user")
    private val system = testApp(packageName = "com.system", isSystem = true)

    @Test
    fun systemAppsHiddenByDefault() {
        assertEquals(listOf(user), visibleApps(listOf(user, system)))
    }

    @Test
    fun systemAppsShownWhenToggledOn() {
        assertEquals(listOf(user, system), visibleApps(listOf(user, system), showSystem = true))
    }

    @Test
    fun emptyListStaysEmpty() {
        assertEquals(emptyList<Any>(), visibleApps(emptyList()))
    }
}
