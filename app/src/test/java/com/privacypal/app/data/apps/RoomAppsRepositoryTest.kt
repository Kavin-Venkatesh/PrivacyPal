package com.privacypal.app.data.apps

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.privacypal.app.data.db.PrivacyPalDatabase
import com.privacypal.app.domain.model.AppCategory
import com.privacypal.app.testing.CAMERA
import com.privacypal.app.testing.FakeInstalledAppsSource
import com.privacypal.app.testing.RECORD_AUDIO
import com.privacypal.app.testing.testApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomAppsRepositoryTest {

    private lateinit var db: PrivacyPalDatabase
    private val source = FakeInstalledAppsSource()
    private lateinit var repository: RoomAppsRepository

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext<Context>(),
                PrivacyPalDatabase::class.java,
            )
                .allowMainThreadQueries()
                .build()
        repository = RoomAppsRepository(db.appDao(), source) { 1_000L }
    }

    @After
    fun tearDown() = db.close()

    private val chat =
        testApp("com.chat", "Chat", AppCategory.SOCIAL, granted = listOf(CAMERA), requestedOnly = listOf(RECORD_AUDIO))
    private val game = testApp("com.game", "Game", AppCategory.GAME, granted = listOf(RECORD_AUDIO))

    @Test
    fun startsEmptyAndNotStarted() = runTest {
        assertTrue(repository.apps.first().isEmpty())
        assertEquals(RefreshState.NOT_STARTED, repository.refreshState.value)
    }

    @Test
    fun refreshStoresScannedAppsRoundTrip() = runTest {
        source.apps = listOf(game, chat)

        repository.refresh()

        assertEquals(listOf(chat, game), repository.apps.first())
        assertEquals(RefreshState.IDLE, repository.refreshState.value)
    }

    @Test
    fun refreshRemovesUninstalledApps() = runTest {
        source.apps = listOf(chat, game)
        repository.refresh()

        source.apps = listOf(chat)
        repository.refresh()

        assertEquals(listOf(chat), repository.apps.first())
    }

    @Test
    fun refreshPicksUpRevokedPermission() = runTest {
        source.apps = listOf(chat)
        repository.refresh()

        source.apps =
            listOf(testApp("com.chat", "Chat", AppCategory.SOCIAL, requestedOnly = listOf(CAMERA, RECORD_AUDIO)))
        repository.refresh()

        assertTrue(repository.app("com.chat").first()!!.grantedGroups.isEmpty())
    }

    @Test
    fun failedScanKeepsOldDataAndReportsFailure() = runTest {
        source.apps = listOf(chat)
        repository.refresh()

        source.failure = IllegalStateException("package manager died")
        repository.refresh()

        assertEquals(RefreshState.FAILED, repository.refreshState.value)
        assertEquals(listOf(chat), repository.apps.first())
    }

    @Test
    fun singleRefreshUpdatesOneApp() = runTest {
        source.apps = listOf(chat, game)
        repository.refresh()
        val updatedGame = testApp("com.game", "Game", AppCategory.GAME, requestedOnly = listOf(RECORD_AUDIO))
        source.apps = listOf(chat, updatedGame)

        repository.refresh("com.game")

        assertEquals(updatedGame, repository.app("com.game").first())
        assertEquals(chat, repository.app("com.chat").first())
    }

    @Test
    fun singleRefreshOfNewPackageAddsIt() = runTest {
        source.apps = listOf(chat)
        repository.refresh()
        source.apps = listOf(chat, game)

        repository.refresh("com.game")

        assertEquals(game, repository.app("com.game").first())
    }

    @Test
    fun singleRefreshOfUninstalledPackageRemovesIt() = runTest {
        source.apps = listOf(chat, game)
        repository.refresh()
        source.apps = listOf(chat)

        repository.refresh("com.game")

        assertNull(repository.app("com.game").first())
    }

    @Test
    fun singleRefreshFailureIsSwallowed() = runTest {
        source.apps = listOf(chat)
        repository.refresh()
        source.failure = IllegalStateException("boom")

        repository.refresh("com.chat")

        assertEquals(chat, repository.app("com.chat").first())
    }
}
