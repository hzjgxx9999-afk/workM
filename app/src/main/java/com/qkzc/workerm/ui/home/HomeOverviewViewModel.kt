package com.qkzc.workerm.ui.home

import androidx.lifecycle.ViewModel
import com.qkzc.workerm.R
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeOverviewViewModel(
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeOverviewUiState>(HomeOverviewUiState.Loading)
    val uiState: StateFlow<HomeOverviewUiState> = _uiState.asStateFlow()

    init {
        loadOverview()
    }

    fun loadOverview() {
        _uiState.value = HomeOverviewUiState.Loading
        runCatching {
            buildSuccessState(defaultCounts)
        }.onSuccess { state ->
            _uiState.value = state
        }.onFailure {
            showOverviewError()
        }
    }

    fun updateOverviewData(counts: HomeOverviewCounts) {
        _uiState.value = buildSuccessState(counts)
    }

    fun showOverviewError() {
        _uiState.value = HomeOverviewUiState.Error(
            messageRes = R.string.home_overview_error_message,
        )
    }

    private fun buildSuccessState(counts: HomeOverviewCounts): HomeOverviewUiState.Success {
        return HomeOverviewUiState.Success(
            dateText = formatOverviewDate(LocalDate.now(clock)),
            items = buildOverviewItems(counts),
        )
    }

    private fun buildOverviewItems(counts: HomeOverviewCounts): List<OverviewItem> {
        return listOf(
            OverviewItem(
                type = OverviewType.UnderConstructionProjects,
                titleRes = R.string.home_overview_project_title,
                quantity = counts.projectCount,
                unitRes = R.string.home_overview_unit_count,
                iconType = OverviewIconType.ProjectDocument,
                iconContentDescriptionRes = R.string.home_overview_project_icon_cd,
            ),
            OverviewItem(
                type = OverviewType.OnSiteWorkers,
                titleRes = R.string.home_overview_worker_title,
                quantity = counts.workerCount,
                unitRes = R.string.home_overview_unit_person,
                iconType = OverviewIconType.WorkerGroup,
                iconContentDescriptionRes = R.string.home_overview_worker_icon_cd,
            ),
            OverviewItem(
                type = OverviewType.TodayApprovals,
                titleRes = R.string.home_overview_approval_title,
                quantity = counts.approvalCount,
                unitRes = R.string.home_overview_unit_item,
                iconType = OverviewIconType.ApprovalDocument,
                iconContentDescriptionRes = R.string.home_overview_approval_icon_cd,
            ),
            OverviewItem(
                type = OverviewType.RiskWarnings,
                titleRes = R.string.home_overview_warning_title,
                quantity = counts.warningCount,
                unitRes = R.string.home_overview_unit_record,
                iconType = OverviewIconType.WarningTriangle,
                iconContentDescriptionRes = R.string.home_overview_warning_icon_cd,
            ),
        )
    }

    private fun formatOverviewDate(date: LocalDate): String {
        return date.format(dateFormatter)
    }

    private companion object {
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd  EEEE", Locale.CHINA)

        val defaultCounts = HomeOverviewCounts(
            projectCount = 12,
            workerCount = 328,
            approvalCount = 15,
            warningCount = 8,
        )
    }
}
