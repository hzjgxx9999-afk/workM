package com.qkzc.workerm

import com.qkzc.workerm.data.approval.ApprovalRepository
import com.qkzc.workerm.data.approval.model.ApprovalCategory
import com.qkzc.workerm.data.approval.model.ApprovalFilter
import com.qkzc.workerm.data.approval.model.ApprovalItem
import com.qkzc.workerm.data.approval.model.ApprovalStatus
import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ApprovalApiConstants
import com.qkzc.workerm.data.network.AuditListReq
import com.qkzc.workerm.data.network.ExitAuditReq
import com.qkzc.workerm.data.network.ExitDetailReq
import com.qkzc.workerm.data.network.ExitListReq
import com.qkzc.workerm.data.network.ExitRequestVo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ApprovalRepositoryExitTest {

    @Test
    fun loadPageIncludesExitRequestsInPendingManagerList() = runBlocking {
        val api = FakeApprovalApi(
            exits = listOf(
                ExitRequestVo(
                    id = 7,
                    exitProcessId = 3,
                    entryId = 2,
                    requestType = "TASK_DONE",
                    requestTypeLabel = "完工确认",
                    requestStatus = ApprovalApiConstants.STATUS_PENDING_MANAGER,
                    statusLabel = "待项目经理审批",
                    applicantId = 501,
                    realName = "张三",
                    projectId = 100,
                    projectName = "中建项目",
                    leaderId = 10,
                    leaderName = "李班组长",
                    applyTime = "2026-06-09 08:30:00",
                    payloadJson = "{\"reason\":\"离场\"}",
                ),
            ),
        )
        val repository = ApprovalRepository(api)

        val (_, approvals) = repository.loadPage(
            token = "token",
            projectId = null,
            filter = ApprovalFilter.PENDING,
        )

        assertEquals("Bearer token", api.lastToken)
        assertEquals(ApprovalCategory.EXIT, approvals.single().category)
        assertEquals(ApprovalStatus.PENDING, approvals.single().status)
        assertEquals("离场审批", approvals.single().typeName)
        assertEquals("张三", approvals.single().applicantName)
        assertEquals("中建项目", approvals.single().projectName)
    }

    @Test
    fun auditDispatchesExitRequestsToExitAuditEndpoint() = runBlocking {
        val api = FakeApprovalApi()
        val repository = ApprovalRepository(api)
        val item = ApprovalItem(
            id = 9,
            category = ApprovalCategory.EXIT,
            formNo = "EXT-9",
            typeName = "离场审批",
            title = "离场申请",
            applicantName = "张三",
            projectName = "中建项目",
            submittedAt = "2026-06-09",
            currentNodeName = "项目经理终审",
            reason = "离场",
            status = ApprovalStatus.PENDING,
        )

        repository.audit(
            token = "token",
            item = item,
            approve = false,
            remark = "资料不完整",
        )

        assertEquals(ExitAuditReq(requestId = 9, action = "REJECT", auditRemark = "资料不完整"), api.lastExitAudit)
    }

    private class FakeApprovalApi(
        private val exits: List<ExitRequestVo> = emptyList(),
    ) : EmptySupervisorApi() {
        var lastToken: String? = null
        var lastExitAudit: ExitAuditReq? = null

        override suspend fun advanceList(token: String, body: AuditListReq) =
            AjaxResp(code = 200, msg = "ok", data = emptyList<com.qkzc.workerm.data.network.AdvanceRequestVo>())

        override suspend fun materialList(
            token: String,
            body: com.qkzc.workerm.data.network.MaterialListReq,
        ) = AjaxResp(code = 200, msg = "ok", data = emptyList<com.qkzc.workerm.data.network.MaterialRequestVo>())

        override suspend fun exceptionList(token: String, body: AuditListReq) =
            AjaxResp(code = 200, msg = "ok", data = emptyList<com.qkzc.workerm.data.network.AttendanceExceptionVo>())

        override suspend fun exitList(token: String, body: ExitListReq): AjaxResp<List<ExitRequestVo>> {
            lastToken = token
            return AjaxResp(code = 200, msg = "ok", data = exits)
        }

        override suspend fun exitDetail(token: String, body: ExitDetailReq): AjaxResp<ExitRequestVo> =
            AjaxResp(code = 200, msg = "ok", data = exits.first())

        override suspend fun auditExit(token: String, body: ExitAuditReq): AjaxResp<ExitRequestVo> {
            lastExitAudit = body
            return AjaxResp(code = 200, msg = "ok", data = null)
        }
    }
}
