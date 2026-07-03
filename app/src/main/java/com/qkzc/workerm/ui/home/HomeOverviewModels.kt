package com.qkzc.workerm.ui.home

import androidx.annotation.StringRes

enum class OverviewType {
    UnderConstructionProjects,
    OnSiteWorkers,
    TodayApprovals,
    RiskWarnings,
}

enum class OverviewIconType {
    ProjectDocument,
    WorkerGroup,
    ApprovalDocument,
    WarningTriangle,
}

data class OverviewItem(
    val type: OverviewType,
    @param:StringRes val titleRes: Int,
    val quantity: Int,
    @param:StringRes val unitRes: Int,
    val iconType: OverviewIconType,
    @param:StringRes val iconContentDescriptionRes: Int,
)

data class HomeOverviewCounts(
    val projectCount: Int,
    val workerCount: Int,
    val approvalCount: Int,
    val warningCount: Int,
)

sealed interface HomeOverviewUiState {
    data object Loading : HomeOverviewUiState

    data class Success(
        val dateText: String,
        val items: List<OverviewItem>,
    ) : HomeOverviewUiState

    data class Error(
        @param:StringRes val messageRes: Int,
    ) : HomeOverviewUiState
}
