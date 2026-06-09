package com.qkzc.workerm

import com.qkzc.workerm.data.approval.ApprovalRepository
import com.qkzc.workerm.data.approval.model.ApprovalFilter
import com.qkzc.workerm.data.network.AdvanceRequestVo
import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ApprovalApiConstants
import com.qkzc.workerm.data.network.AttendanceExceptionVo
import com.qkzc.workerm.data.network.AuditListReq
import com.qkzc.workerm.data.network.ExitListReq
import com.qkzc.workerm.data.network.ExitRequestVo
import com.qkzc.workerm.data.network.MaterialListReq
import com.qkzc.workerm.data.network.MaterialRequestVo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ApprovalRepositoryDisplayFieldsTest {

    @Test
    fun mapsRealWorkerProjectTeamAndLeaderNamesWithoutIdFallback() = runBlocking {
        val repository = ApprovalRepository(
            DisplayFakeApi(
                advances = listOf(
                    AdvanceRequestVo(
                        id = 11,
                        userId = 31,
                        realName = "张伟",
                        mobile = "138****5678",
                        projectId = 5,
                        projectName = "中建·未来城二期项目",
                        projectAddress = "广州市天河区珠江新城中建路888号",
                        contractorUnit = "中建五局",
                        leaderId = 10,
                        leaderName = "刘师傅",
                        teamName = "钢筋班",
                        amount = 1500.0,
                        reason = "生活费用",
                        status = ApprovalApiConstants.STATUS_PENDING_MANAGER,
                        statusLabel = "待项目经理审批",
                        currentNodeLabel = "项目经理终审",
                        leaderAuditStatus = "APPROVED",
                        leaderAuditStatusLabel = "同意",
                        managerAuditStatus = null,
                        managerAuditStatusLabel = null,
                        leaderAuditUserId = 10,
                        leaderAuditTime = "2026-06-09 09:00:00",
                        leaderAuditRemark = "同意",
                    ),
                ),
                materials = listOf(
                    MaterialRequestVo(
                        id = 12,
                        userId = 32,
                        realName = "李强",
                        mobile = "139****2468",
                        projectId = 5,
                        projectName = "中建·未来城二期项目",
                        projectAddress = "广州市天河区珠江新城中建路888号",
                        contractorUnit = "中建五局",
                        leaderId = 10,
                        leaderName = "刘师傅",
                        teamName = "钢筋班",
                        itemName = "安全帽",
                        quantity = 5.0,
                        unit = "顶",
                        reason = "新增工人",
                        status = ApprovalApiConstants.STATUS_PENDING_MANAGER,
                        statusLabel = "待项目经理审批",
                        currentNodeLabel = "项目经理终审",
                        leaderAuditStatus = "APPROVED",
                        leaderAuditStatusLabel = "同意",
                        managerAuditStatus = null,
                        managerAuditStatusLabel = null,
                    ),
                ),
                exceptions = listOf(
                    AttendanceExceptionVo(
                        id = 13,
                        userId = 33,
                        realName = "王建国",
                        mobile = "137****9988",
                        projectId = 5,
                        projectName = "中建·未来城二期项目",
                        projectAddress = "广州市天河区珠江新城中建路888号",
                        contractorUnit = "中建五局",
                        leaderId = 10,
                        leaderName = "刘师傅",
                        teamName = "钢筋班",
                        attendanceId = 99,
                        exceptionType = "MISSING_CHECK_IN",
                        exceptionTypeLabel = "上班缺卡",
                        reason = "上班补卡测试 班组，管理",
                        workDate = "2026-06-09",
                        fixCheckInTime = null,
                        fixCheckOutTime = null,
                        status = ApprovalApiConstants.STATUS_PENDING_MANAGER,
                        statusLabel = "待项目经理审批",
                        currentNodeLabel = "项目经理终审",
                        leaderAuditStatus = "APPROVED",
                        leaderAuditStatusLabel = "同意",
                        managerAuditStatus = null,
                        managerAuditStatusLabel = null,
                    ),
                ),
                exits = listOf(
                    ExitRequestVo(
                        id = 14,
                        exitProcessId = 9,
                        entryId = 8,
                        projectId = 5,
                        projectName = "中建·未来城二期项目",
                        projectAddress = "广州市天河区珠江新城中建路888号",
                        contractorUnit = "中建五局",
                        leaderId = 10,
                        leaderName = "刘师傅",
                        teamName = "钢筋班",
                        requestType = "TASK_DONE",
                        requestTypeLabel = "完工确认",
                        requestStatus = ApprovalApiConstants.STATUS_PENDING_MANAGER,
                        statusLabel = "待项目经理审批",
                        applicantId = 34,
                        realName = "赵刚",
                        mobile = "136****1111",
                        payloadJson = "离场申请",
                        applyTime = "2026-06-09 10:00:00",
                    ),
                ),
            ),
        )

        val (_, approvals) = repository.loadPage("token", projectId = null, filter = ApprovalFilter.PENDING)

        assertEquals(4, approvals.size)
        approvals.forEach { item ->
            assertFalse(item.applicantName.startsWith("工人 "))
            assertFalse(item.projectName.startsWith("项目 "))
            assertEquals("中建·未来城二期项目", item.projectName)
            assertEquals("广州市天河区珠江新城中建路888号", item.projectAddress)
            assertEquals("中建五局", item.contractorUnit)
            assertEquals("钢筋班", item.teamName)
            assertEquals("刘师傅", item.leaderName)
        }
        assertEquals("张伟", approvals.first { it.id == 11L }.applicantName)
        assertEquals("王建国", approvals.first { it.id == 13L }.applicantName)
    }

    private class DisplayFakeApi(
        private val advances: List<AdvanceRequestVo> = emptyList(),
        private val materials: List<MaterialRequestVo> = emptyList(),
        private val exceptions: List<AttendanceExceptionVo> = emptyList(),
        private val exits: List<ExitRequestVo> = emptyList(),
    ) : EmptySupervisorApi() {
        override suspend fun advanceList(token: String, body: AuditListReq) =
            AjaxResp(code = 200, msg = "ok", data = advances)

        override suspend fun materialList(token: String, body: MaterialListReq) =
            AjaxResp(code = 200, msg = "ok", data = materials)

        override suspend fun exceptionList(token: String, body: AuditListReq) =
            AjaxResp(code = 200, msg = "ok", data = exceptions)

        override suspend fun exitList(token: String, body: ExitListReq) =
            AjaxResp(code = 200, msg = "ok", data = exits)
    }
}
