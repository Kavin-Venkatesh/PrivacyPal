package com.privacypal.app.ui.apps

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.privacypal.app.data.apps.RefreshState
import com.privacypal.app.domain.apps.AppFilter
import com.privacypal.app.domain.apps.AppQuery
import com.privacypal.app.domain.apps.AppSort
import com.privacypal.app.testing.FakeAppsRepository
import com.privacypal.app.testing.MainDispatcherRule
import com.privacypal.app.testing.sampleApps
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun content(state: AppsUiState) = state as AppsUiState.Content

    @Test
    fun loadingUntilFirstScanFinishes() = runTest {
        val repository = FakeAppsRepository(refreshState = RefreshState.RUNNING)
        val viewModel = AppsViewModel(repository, SavedStateHandle())

        viewModel.uiState.test {
            assertEquals(AppsUiState.Loading, awaitItem())
            repository.appsFlow.value = sampleApps
            repository.refreshState.value = RefreshState.IDLE
            assertEquals(5, content(expectMostRecentItem()).rows.size)
        }
    }

    @Test
    fun errorWhenFirstScanFailsWithNothingStored() = runTest {
        val repository = FakeAppsRepository(refreshState = RefreshState.FAILED)
        val viewModel = AppsViewModel(repository, SavedStateHandle())

        viewModel.uiState.test {
            assertEquals(AppsUiState.Error, expectMostRecentItem())
            viewModel.retry()
            assertEquals(1, repository.refreshCount)
        }
    }

    @Test
    fun failedRescanKeepsStoredAppsAndFlagsIt() = runTest {
        val repository = FakeAppsRepository(sampleApps, RefreshState.FAILED)
        val viewModel = AppsViewModel(repository, SavedStateHandle())

        viewModel.uiState.test {
            val state = content(expectMostRecentItem())
            assertTrue(state.refreshFailed)
            assertEquals(5, state.rows.size)
        }
    }

    @Test
    fun emptyListAfterSuccessfulScanIsContentNotLoading() = runTest {
        val viewModel = AppsViewModel(FakeAppsRepository(refreshState = RefreshState.IDLE), SavedStateHandle())

        viewModel.uiState.test {
            assertTrue(content(expectMostRecentItem()).rows.isEmpty())
        }
    }

    @Test
    fun queryChangesUpdateRowsAndAreSaved() = runTest {
        val handle = SavedStateHandle()
        val viewModel = AppsViewModel(FakeAppsRepository(sampleApps), handle)

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onSearchChange("chat")
            assertEquals(listOf("Chatter"), content(expectMostRecentItem()).rows.map { it.app.label })
            viewModel.onSearchChange("")
            viewModel.onSortChange(AppSort.NAME)
            assertEquals(
                listOf("Bright Torch", "Calculator", "Chatter", "Notes", "Weather Now"),
                content(expectMostRecentItem()).rows.map { it.app.label },
            )
            viewModel.onFilterChange(AppFilter.HIGH_RISK)
            assertEquals(
                listOf("Bright Torch", "Weather Now"),
                content(expectMostRecentItem()).rows.map { it.app.label },
            )
            viewModel.onShowSystemChange(true)
            assertEquals(3, content(expectMostRecentItem()).rows.size)
        }
        assertEquals(AppFilter.HIGH_RISK.name, handle.get<String>("filter"))
        assertEquals(AppSort.NAME.name, handle.get<String>("sort"))
        assertEquals(true, handle.get<Boolean>("showSystem"))
    }

    @Test
    fun queryIsRestoredFromSavedState() = runTest {
        val handle = SavedStateHandle(
            mapOf("search" to "torch", "filter" to "CAMERA", "sort" to "NAME", "showSystem" to true),
        )
        val viewModel = AppsViewModel(FakeAppsRepository(sampleApps), handle)

        assertEquals(AppQuery("torch", AppFilter.CAMERA, AppSort.NAME, showSystem = true), viewModel.query.value)
        viewModel.uiState.test {
            assertEquals(listOf("Bright Torch"), content(expectMostRecentItem()).rows.map { it.app.label })
        }
    }

    @Test
    fun unknownSavedValuesFallBackToDefaults() {
        val handle = SavedStateHandle(mapOf("filter" to "GONE", "sort" to "ALSO_GONE"))

        assertEquals(AppQuery(), AppsViewModel(FakeAppsRepository(), handle).query.value)
    }

    @Test
    fun clearFiltersResetsSearchAndFilterButKeepsSortAndSystemToggle() = runTest {
        val viewModel = AppsViewModel(FakeAppsRepository(sampleApps), SavedStateHandle())
        viewModel.onSearchChange("x")
        viewModel.onFilterChange(AppFilter.CAMERA)
        viewModel.onSortChange(AppSort.NAME)
        viewModel.onShowSystemChange(true)

        viewModel.clearFilters()

        assertEquals(AppQuery(sort = AppSort.NAME, showSystem = true), viewModel.query.value)
    }

    @Test
    fun summaryFollowsSystemToggle() = runTest {
        val viewModel = AppsViewModel(FakeAppsRepository(sampleApps), SavedStateHandle())

        viewModel.uiState.test {
            assertEquals(5, content(expectMostRecentItem()).summary.total)
            viewModel.onShowSystemChange(true)
            assertEquals(6, content(expectMostRecentItem()).summary.total)
        }
    }
}
