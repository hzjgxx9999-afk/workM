package com.qkzc.workerm

import com.qkzc.workerm.ui.home.HomeOverviewCounts
import com.qkzc.workerm.ui.home.HomeOverviewUiState
import com.qkzc.workerm.ui.home.HomeOverviewViewModel
import com.qkzc.workerm.ui.home.OverviewType
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeOverviewViewModelTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2024-05-20T02:30:00Z"),
        ZoneId.of("Asia/Shanghai"),
    )

    @Test
    fun defaultStateBuildsDateAndFourOverviewItems() {
        val viewModel = HomeOverviewViewModel(clock = fixedClock)

        val state = viewModel.uiState.value

        assertTrue(state is HomeOverviewUiState.Success)
        state as HomeOverviewUiState.Success
        assertEquals("2024-05-20  \u661f\u671f\u4e00", state.dateText)
        assertEquals(
            listOf(
                OverviewType.UnderConstructionProjects,
                OverviewType.OnSiteWorkers,
                OverviewType.TodayApprovals,
                OverviewType.RiskWarnings,
            ),
            state.items.map { it.type },
        )
        assertEquals(12, state.items[0].quantity)
        assertEquals(R.string.home_overview_unit_count, state.items[0].unitRes)
        assertEquals(328, state.items[1].quantity)
        assertEquals(R.string.home_overview_unit_person, state.items[1].unitRes)
        assertEquals(15, state.items[2].quantity)
        assertEquals(R.string.home_overview_unit_item, state.items[2].unitRes)
        assertEquals(8, state.items[3].quantity)
        assertEquals(R.string.home_overview_unit_record, state.items[3].unitRes)
    }

    @Test
    fun updateOverviewDataReplacesCardQuantitiesWithoutChangingModelShape() {
        val viewModel = HomeOverviewViewModel(clock = fixedClock)

        viewModel.updateOverviewData(
            HomeOverviewCounts(
                projectCount = 7,
                workerCount = 96,
                approvalCount = 4,
                warningCount = 2,
            ),
        )

        val state = viewModel.uiState.value as HomeOverviewUiState.Success
        assertEquals(listOf(7, 96, 4, 2), state.items.map { it.quantity })
        assertEquals(4, state.items.distinctBy { it.type }.size)
    }

    @Test
    fun showErrorKeepsRecoverableErrorState() {
        val viewModel = HomeOverviewViewModel(clock = fixedClock)

        viewModel.showOverviewError()

        val state = viewModel.uiState.value
        assertTrue(state is HomeOverviewUiState.Error)
        state as HomeOverviewUiState.Error
        assertEquals(R.string.home_overview_error_message, state.messageRes)
    }
}
