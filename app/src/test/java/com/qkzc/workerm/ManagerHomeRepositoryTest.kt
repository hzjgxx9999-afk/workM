package com.qkzc.workerm

import com.qkzc.workerm.data.home.ManagerHomeRepository
import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ManagerApprovalSummaryVo
import com.qkzc.workerm.data.network.ManagerHomeOverviewVo
import com.qkzc.workerm.data.network.ManagerRiskSummaryVo
import com.qkzc.workerm.data.network.SupervisorApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ManagerHomeRepositoryTest {

    @Test
    fun overviewMapsAggregatedCountsForHomeCards() = runBlocking {
        val api = FakeHomeApi(
            response = ManagerHomeOverviewVo(
                currentProjectId = 10L,
                projectCount = 12,
                workerCount = 328,
                approvalSummary = ManagerApprovalSummaryVo(
                    pendingCount = 15L,
                    todaySubmittedCount = 6L,
                    todayProcessedCount = 4L,
                ),
                riskSummary = ManagerRiskSummaryVo(
                    unhandledRiskCount = 8L,
                    highRiskCount = 2L,
                    unreadRiskCount = 3L,
                ),
            ),
        )
        val repository = ManagerHomeRepository(api)

        val overview = repository.overview("token")

        assertEquals("Bearer token", api.lastToken)
        assertEquals(null, api.lastProjectId)
        assertEquals(10L, overview.currentProjectId)
        assertEquals(12, overview.projectCount)
        assertEquals(328, overview.workerCount)
        assertEquals(15, overview.pendingApprovalCount)
        assertEquals(6, overview.todaySubmittedApprovalCount)
        assertEquals(4, overview.todayProcessedApprovalCount)
        assertEquals(8, overview.unhandledWarningCount)
        assertEquals(2, overview.highRiskCount)
        assertEquals(3, overview.unreadWarningCount)
    }

    @Test
    fun overviewPassesOptionalProjectId() = runBlocking {
        val api = FakeHomeApi(response = ManagerHomeOverviewVo(projectCount = 1))
        val repository = ManagerHomeRepository(api)

        repository.overview("token", projectId = 10L)

        assertEquals(10L, api.lastProjectId)
    }

    private class FakeHomeApi(
        private val response: ManagerHomeOverviewVo,
    ) : SupervisorApi by EmptySupervisorApi() {
        var lastToken: String? = null
        var lastProjectId: Long? = null

        override suspend fun manageHomeOverview(
            token: String,
            projectId: Long?,
        ): AjaxResp<ManagerHomeOverviewVo> {
            lastToken = token
            lastProjectId = projectId
            return AjaxResp(code = 200, msg = "ok", data = response)
        }
    }
}
