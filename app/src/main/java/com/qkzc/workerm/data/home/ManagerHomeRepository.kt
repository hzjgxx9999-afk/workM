package com.qkzc.workerm.data.home

import com.qkzc.workerm.data.network.ApiClient
import com.qkzc.workerm.data.network.ManagerHomeOverviewVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.network.bearerToken
import com.qkzc.workerm.data.network.requireSuccess

class ManagerHomeRepository(
    private val api: SupervisorApi = ApiClient.supervisorApi,
) {
    suspend fun overview(token: String, projectId: Long? = null): ManagerHomeOverview {
        val response = api.manageHomeOverview(
            token = bearerToken(token),
            projectId = projectId,
        )
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: ManagerHomeOverview()
    }

    private fun ManagerHomeOverviewVo.toDomain(): ManagerHomeOverview {
        return ManagerHomeOverview(
            currentProjectId = currentProjectId ?: riskSummary?.currentProjectId,
            projectCount = projectCount ?: projects.orEmpty().size,
            workerCount = workerCount ?: 0,
            pendingApprovalCount = approvalSummary?.pendingCount.toSafeInt(),
            todaySubmittedApprovalCount = approvalSummary?.todaySubmittedCount.toSafeInt(),
            todayProcessedApprovalCount = approvalSummary?.todayProcessedCount.toSafeInt(),
            unhandledWarningCount = riskSummary?.unhandledRiskCount.toSafeInt(),
            highRiskCount = riskSummary?.highRiskCount.toSafeInt(),
            unreadWarningCount = riskSummary?.unreadRiskCount.toSafeInt(),
        )
    }

    private fun Long?.toSafeInt(): Int {
        return this?.coerceIn(0L, Int.MAX_VALUE.toLong())?.toInt() ?: 0
    }
}

data class ManagerHomeOverview(
    val currentProjectId: Long? = null,
    val projectCount: Int = 0,
    val workerCount: Int = 0,
    val pendingApprovalCount: Int = 0,
    val todaySubmittedApprovalCount: Int = 0,
    val todayProcessedApprovalCount: Int = 0,
    val unhandledWarningCount: Int = 0,
    val highRiskCount: Int = 0,
    val unreadWarningCount: Int = 0,
)
