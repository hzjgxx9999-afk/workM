package com.qkzc.workerm

import com.qkzc.workerm.data.network.AdvanceAuditReq
import com.qkzc.workerm.data.network.AdvanceDetailReq
import com.qkzc.workerm.data.network.AdvanceRequestVo
import com.qkzc.workerm.data.network.AiWarningDetailReq
import com.qkzc.workerm.data.network.AiWarningHandleReq
import com.qkzc.workerm.data.network.AiWarningListReq
import com.qkzc.workerm.data.network.AiWarningPageVo
import com.qkzc.workerm.data.network.AiWarningReadReq
import com.qkzc.workerm.data.network.AiWarningVo
import com.qkzc.workerm.data.network.AjaxProfileResp
import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.AjaxTokenResp
import com.qkzc.workerm.data.network.AttendanceExceptionVo
import com.qkzc.workerm.data.network.AuditListReq
import com.qkzc.workerm.data.network.ExceptionAuditReq
import com.qkzc.workerm.data.network.ExceptionDetailReq
import com.qkzc.workerm.data.network.LoginReq
import com.qkzc.workerm.data.network.ManagerHomeOverviewVo
import com.qkzc.workerm.data.network.ManagerProjectVo
import com.qkzc.workerm.data.network.ManagerTeamLeaderVo
import com.qkzc.workerm.data.network.ManagerWorkerScanReq
import com.qkzc.workerm.data.network.ManagerWorkerVo
import com.qkzc.workerm.data.network.MaterialAuditReq
import com.qkzc.workerm.data.network.MaterialDetailReq
import com.qkzc.workerm.data.network.MaterialListReq
import com.qkzc.workerm.data.network.MaterialRequestVo
import com.qkzc.workerm.data.network.SupervisorApi

open class EmptySupervisorApi : SupervisorApi {
    override suspend fun login(body: LoginReq): AjaxTokenResp = error("unused")
    override suspend fun profile(token: String): AjaxProfileResp = error("unused")
    override suspend fun manageProjects(token: String): AjaxResp<List<ManagerProjectVo>> = error("unused")
    override suspend fun manageProjectDetail(token: String, projectId: Long): AjaxResp<ManagerProjectVo> = error("unused")
    override suspend fun manageProjectTeamLeaders(token: String, projectId: Long): AjaxResp<List<ManagerTeamLeaderVo>> = error("unused")
    override suspend fun manageHomeOverview(token: String, projectId: Long?): AjaxResp<ManagerHomeOverviewVo> = error("unused")
    override suspend fun manageWorkers(token: String, projectId: Long?, status: String?, keyword: String?): AjaxResp<List<ManagerWorkerVo>> = error("unused")
    override suspend fun manageWorkerDetail(token: String, workerId: Long, projectId: Long): AjaxResp<ManagerWorkerVo> = error("unused")
    override suspend fun manageWorkerScanTicket(token: String, body: ManagerWorkerScanReq): AjaxResp<ManagerWorkerVo> = error("unused")
    override suspend fun advanceList(token: String, body: AuditListReq): AjaxResp<List<AdvanceRequestVo>> = error("unused")
    override suspend fun advanceDetail(token: String, body: AdvanceDetailReq): AjaxResp<AdvanceRequestVo> = error("unused")
    override suspend fun auditAdvance(token: String, body: AdvanceAuditReq): AjaxResp<AdvanceRequestVo> = error("unused")
    override suspend fun materialList(token: String, body: MaterialListReq): AjaxResp<List<MaterialRequestVo>> = error("unused")
    override suspend fun materialDetail(token: String, body: MaterialDetailReq): AjaxResp<MaterialRequestVo> = error("unused")
    override suspend fun auditMaterial(token: String, body: MaterialAuditReq): AjaxResp<MaterialRequestVo> = error("unused")
    override suspend fun exceptionList(token: String, body: AuditListReq): AjaxResp<List<AttendanceExceptionVo>> = error("unused")
    override suspend fun exceptionDetail(token: String, body: ExceptionDetailReq): AjaxResp<AttendanceExceptionVo> = error("unused")
    override suspend fun auditException(token: String, body: ExceptionAuditReq): AjaxResp<AttendanceExceptionVo> = error("unused")
    override suspend fun manageAiWarningList(token: String, body: AiWarningListReq): AjaxResp<AiWarningPageVo> = error("unused")
    override suspend fun manageAiWarningDetail(token: String, body: AiWarningDetailReq): AjaxResp<AiWarningVo> = error("unused")
    override suspend fun manageAiWarningRead(token: String, body: AiWarningReadReq): AjaxResp<Any> = error("unused")
    override suspend fun manageAiWarningHandle(token: String, body: AiWarningHandleReq): AjaxResp<Any> = error("unused")
    override suspend fun manageAiWarningUnreadCount(token: String, body: Map<String, String>): AjaxResp<Int> = error("unused")
}
