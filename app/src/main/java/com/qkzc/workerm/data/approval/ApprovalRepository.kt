package com.qkzc.workerm.data.approval

import com.qkzc.workerm.data.approval.model.ApprovalCategory
import com.qkzc.workerm.data.approval.model.ApprovalFilter
import com.qkzc.workerm.data.approval.model.ApprovalItem
import com.qkzc.workerm.data.approval.model.ApprovalStatus
import com.qkzc.workerm.data.approval.model.ApprovalSummary
import com.qkzc.workerm.data.network.AdvanceAuditReq
import com.qkzc.workerm.data.network.AdvanceRequestVo
import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ApiClient
import com.qkzc.workerm.data.network.ApprovalApiConstants
import com.qkzc.workerm.data.network.AttendanceExceptionVo
import com.qkzc.workerm.data.network.AuditListReq
import com.qkzc.workerm.data.network.ExceptionAuditReq
import com.qkzc.workerm.data.network.ExceptionDetailReq
import com.qkzc.workerm.data.network.ExitAuditReq
import com.qkzc.workerm.data.network.ExitDetailReq
import com.qkzc.workerm.data.network.ExitListReq
import com.qkzc.workerm.data.network.ExitRequestVo
import com.qkzc.workerm.data.network.AdvanceDetailReq
import com.qkzc.workerm.data.network.MaterialDetailReq
import com.qkzc.workerm.data.network.MaterialAuditReq
import com.qkzc.workerm.data.network.MaterialListReq
import com.qkzc.workerm.data.network.MaterialRequestVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.network.bearerToken
import com.qkzc.workerm.data.network.requireSuccess

class ApprovalRepository(
    private val api: SupervisorApi = ApiClient.supervisorApi,
) {

    suspend fun loadPage(
        token: String,
        projectId: Long?,
        filter: ApprovalFilter,
    ): Pair<ApprovalSummary, List<ApprovalItem>> {
        val pending = listByStatus(
            token = token,
            projectId = projectId,
            status = ApprovalApiConstants.STATUS_PENDING_MANAGER,
        ).filter { it.status == ApprovalStatus.PENDING }
        val processed = listByStatus(
            token = token,
            projectId = projectId,
            status = ApprovalApiConstants.STATUS_ALL,
        ).filter { it.status != ApprovalStatus.PENDING }

        val summary = ApprovalSummary(
            pendingCount = pending.size,
            processedCount = processed.size,
            rejectedCount = processed.count { it.status == ApprovalStatus.REJECTED },
        )
        val approvals = when (filter) {
            ApprovalFilter.PENDING -> pending
            ApprovalFilter.PROCESSED -> processed
        }
        return summary to approvals
    }

    suspend fun list(
        token: String,
        projectId: Long?,
        filter: ApprovalFilter,
    ): List<ApprovalItem> {
        val status = when (filter) {
            ApprovalFilter.PENDING -> ApprovalApiConstants.STATUS_PENDING_MANAGER
            ApprovalFilter.PROCESSED -> ApprovalApiConstants.STATUS_ALL
        }
        return listByStatus(token, projectId, status).filterByTab(filter)
    }

    private suspend fun listByStatus(
        token: String,
        projectId: Long?,
        status: String,
    ): List<ApprovalItem> {
        val auth = bearerToken(token)
        val advances = api.advanceList(
            token = auth,
            body = AuditListReq(projectId = projectId, status = status),
        ).requireList().map { it.toApprovalItem() }
        val materials = api.materialList(
            token = auth,
            body = MaterialListReq(projectId = projectId, status = status),
        ).requireList().map { it.toApprovalItem() }
        val exceptions = api.exceptionList(
            token = auth,
            body = AuditListReq(projectId = projectId, status = status),
        ).requireList().map { it.toApprovalItem() }
        val exits = api.exitList(
            token = auth,
            body = ExitListReq(projectId = projectId, status = status),
        ).requireList().map { it.toApprovalItem() }

        return advances + materials + exceptions + exits
    }

    suspend fun summary(token: String, projectId: Long?): ApprovalSummary {
        val pending = list(token, projectId, ApprovalFilter.PENDING)
        val processed = list(token, projectId, ApprovalFilter.PROCESSED)
            .filter { it.status != ApprovalStatus.PENDING }
        return ApprovalSummary(
            pendingCount = pending.size,
            processedCount = processed.size,
            rejectedCount = processed.count { it.status == ApprovalStatus.REJECTED },
        )
    }

    suspend fun audit(
        token: String,
        item: ApprovalItem,
        approve: Boolean,
        remark: String,
    ) {
        val action = if (approve) {
            ApprovalApiConstants.ACTION_APPROVE
        } else {
            ApprovalApiConstants.ACTION_REJECT
        }
        val auth = bearerToken(token)
        when (item.category) {
            ApprovalCategory.ADVANCE -> api.auditAdvance(
                token = auth,
                body = AdvanceAuditReq(
                    requestId = item.id,
                    action = action,
                    auditRemark = remark,
                ),
            ).requireOk()

            ApprovalCategory.MATERIAL -> api.auditMaterial(
                token = auth,
                body = MaterialAuditReq(
                    requestId = item.id,
                    action = action,
                    auditRemark = remark,
                ),
            ).requireOk()

            ApprovalCategory.EXCEPTION -> api.auditException(
                token = auth,
                body = ExceptionAuditReq(
                    exceptionId = item.id,
                    action = action,
                    auditRemark = remark,
                ),
            ).requireOk()

            ApprovalCategory.EXIT -> api.auditExit(
                token = auth,
                body = ExitAuditReq(
                    requestId = item.id,
                    action = action,
                    auditRemark = remark,
                ),
            ).requireOk()

            ApprovalCategory.LEAVE -> error("请假审批接口尚未接入")
        }
    }

    suspend fun detail(
        token: String,
        category: ApprovalCategory,
        id: Long,
    ): ApprovalItem {
        val auth = bearerToken(token)
        return when (category) {
            ApprovalCategory.ADVANCE -> api.advanceDetail(
                token = auth,
                body = AdvanceDetailReq(id),
            ).requireData().toApprovalItem()

            ApprovalCategory.MATERIAL -> api.materialDetail(
                token = auth,
                body = MaterialDetailReq(id),
            ).requireData().toApprovalItem()

            ApprovalCategory.EXCEPTION -> api.exceptionDetail(
                token = auth,
                body = ExceptionDetailReq(id),
            ).requireData().toApprovalItem()

            ApprovalCategory.EXIT -> api.exitDetail(
                token = auth,
                body = ExitDetailReq(id),
            ).requireData().toApprovalItem()

            ApprovalCategory.LEAVE -> error("请假审批接口尚未接入")
        }
    }

    private fun List<ApprovalItem>.filterByTab(filter: ApprovalFilter): List<ApprovalItem> {
        return when (filter) {
            ApprovalFilter.PENDING -> filter { it.status == ApprovalStatus.PENDING }
            ApprovalFilter.PROCESSED -> filter { it.status != ApprovalStatus.PENDING }
        }
    }

    private fun AdvanceRequestVo.toApprovalItem(): ApprovalItem {
        val approvalStatus = status.toApprovalStatus()
        return ApprovalItem(
            id = id,
            category = ApprovalCategory.ADVANCE,
            formNo = "ADV-$id",
            typeName = "借支审批",
            title = "借支金额 ${amount?.toPlainText().orEmpty()} 元",
            applicantName = realName.displayText() ?: userId?.let { "工人 $it" }.orEmpty(),
            applicantMobile = mobile.displayText().orEmpty(),
            projectName = projectName.displayText() ?: projectId?.let { "项目 $it" }.orEmpty(),
            projectAddress = projectAddress.displayText().orEmpty(),
            contractorUnit = contractorUnit.displayText().orEmpty(),
            projectStatusName = projectStatus.toProjectStatusName(),
            teamName = teamName.displayText().orEmpty(),
            leaderName = leaderName.displayText().orEmpty(),
            submittedAt = leaderAuditTime.orEmpty(),
            currentNodeName = currentNodeLabel.displayText() ?: "项目经理终审",
            reason = reason.orEmpty(),
            status = approvalStatus,
            statusName = statusLabel.displayText() ?: approvalStatus.toDisplayName(),
            reviewResultName = managerAuditStatusLabel.displayText()
                ?: leaderAuditStatusLabel.displayText()
                ?: statusLabel.displayText()
                ?: approvalStatus.toDisplayName(),
            reviewerName = leaderName.displayText() ?: leaderAuditUserId?.let { "班组长 $it" },
            reviewedAt = leaderAuditTime,
            reviewRemark = leaderAuditRemark,
        )
    }

    private fun MaterialRequestVo.toApprovalItem(): ApprovalItem {
        val approvalStatus = status.toApprovalStatus()
        val quantityText = listOfNotNull(quantity?.toPlainText(), unit)
            .joinToString(separator = "")
        return ApprovalItem(
            id = id,
            category = ApprovalCategory.MATERIAL,
            formNo = "MAT-$id",
            typeName = "物资审批",
            title = listOfNotNull(itemName, quantityText.takeIf { it.isNotBlank() })
                .joinToString(separator = " · "),
            applicantName = realName.displayText() ?: userId?.let { "工人 $it" }.orEmpty(),
            applicantMobile = mobile.displayText().orEmpty(),
            projectName = projectName.displayText() ?: projectId?.let { "项目 $it" }.orEmpty(),
            projectAddress = projectAddress.displayText().orEmpty(),
            contractorUnit = contractorUnit.displayText().orEmpty(),
            projectStatusName = projectStatus.toProjectStatusName(),
            teamName = teamName.displayText().orEmpty(),
            leaderName = leaderName.displayText().orEmpty(),
            submittedAt = "",
            currentNodeName = currentNodeLabel.displayText() ?: "项目经理终审",
            reason = reason.orEmpty(),
            status = approvalStatus,
            statusName = statusLabel.displayText() ?: approvalStatus.toDisplayName(),
            reviewResultName = managerAuditStatusLabel.displayText()
                ?: leaderAuditStatusLabel.displayText()
                ?: statusLabel.displayText()
                ?: approvalStatus.toDisplayName(),
        )
    }

    private fun AttendanceExceptionVo.toApprovalItem(): ApprovalItem {
        val approvalStatus = status.toApprovalStatus()
        val exceptionTypeName = exceptionTypeLabel.displayText()
            ?: exceptionType.toExceptionTypeDisplayName()
        return ApprovalItem(
            id = id,
            category = ApprovalCategory.EXCEPTION,
            formNo = "EXC-$id",
            typeName = "补卡审批",
            title = listOfNotNull(exceptionTypeName, workDate).joinToString(separator = " · "),
            applicantName = realName.displayText() ?: userId?.let { "工人 $it" }.orEmpty(),
            applicantMobile = mobile.displayText().orEmpty(),
            projectName = projectName.displayText() ?: projectId?.let { "项目 $it" }.orEmpty(),
            projectAddress = projectAddress.displayText().orEmpty(),
            contractorUnit = contractorUnit.displayText().orEmpty(),
            projectStatusName = projectStatus.toProjectStatusName(),
            teamName = teamName.displayText().orEmpty(),
            leaderName = leaderName.displayText().orEmpty(),
            submittedAt = workDate.orEmpty(),
            currentNodeName = currentNodeLabel.displayText() ?: "项目经理终审",
            reason = reason.orEmpty(),
            status = approvalStatus,
            statusName = statusLabel.displayText() ?: approvalStatus.toDisplayName(),
            reviewResultName = managerAuditStatusLabel.displayText()
                ?: leaderAuditStatusLabel.displayText()
                ?: statusLabel.displayText()
                ?: approvalStatus.toDisplayName(),
        )
    }

    private fun ExitRequestVo.toApprovalItem(): ApprovalItem {
        val approvalStatus = requestStatus.toApprovalStatus()
        val typeDisplay = requestTypeLabel.displayText() ?: requestType.toExitRequestTypeName()
        return ApprovalItem(
            id = id,
            category = ApprovalCategory.EXIT,
            formNo = "EXT-$id",
            typeName = "离场审批",
            title = listOfNotNull(typeDisplay, exitProcessId?.let { "流程 $it" })
                .joinToString(separator = " · "),
            applicantName = realName.displayText() ?: applicantId?.let { "工人 $it" }.orEmpty(),
            applicantMobile = mobile.displayText().orEmpty(),
            projectName = projectName.displayText() ?: projectId?.let { "项目 $it" }.orEmpty(),
            projectAddress = projectAddress.displayText().orEmpty(),
            contractorUnit = contractorUnit.displayText().orEmpty(),
            projectStatusName = projectStatus.toProjectStatusName(),
            teamName = teamName.displayText().orEmpty(),
            leaderName = leaderName.displayText().orEmpty(),
            submittedAt = applyTime.orEmpty(),
            currentNodeName = "项目经理终审",
            reason = payloadJson.displayText() ?: "离场流程申请",
            status = approvalStatus,
            statusName = statusLabel.displayText() ?: approvalStatus.toDisplayName(),
            reviewResultName = managerAuditStatus.toApprovalStatus().toDisplayName(),
            reviewerName = managerAuditUserId?.let { "项目经理 $it" }
                ?: leaderName.displayText(),
            reviewedAt = managerAuditTime ?: approveTime,
            reviewRemark = managerAuditRemark ?: approveRemark,
        )
    }

    private fun String?.toApprovalStatus(): ApprovalStatus {
        return when (this) {
            "APPROVED" -> ApprovalStatus.APPROVED
            "REJECTED" -> ApprovalStatus.REJECTED
            else -> ApprovalStatus.PENDING
        }
    }

    private fun ApprovalStatus.toDisplayName(): String {
        return when (this) {
            ApprovalStatus.PENDING -> "待审批"
            ApprovalStatus.APPROVED -> "同意"
            ApprovalStatus.REJECTED -> "驳回"
        }
    }

    private fun String?.toExceptionTypeDisplayName(): String? {
        return when (this) {
            "MISSING_CHECK_IN" -> "上班缺卡"
            "MISSING_CHECK_OUT" -> "下班缺卡"
            "LATE" -> "迟到"
            "EARLY_LEAVE" -> "早退"
            "ABSENT" -> "缺勤"
            else -> displayText()
        }
    }

    private fun String?.toExitRequestTypeName(): String {
        return when (this) {
            "TASK_DONE" -> "完工确认"
            "MATERIAL_CHECK" -> "物资清点"
            "SALARY_SETTLEMENT" -> "工资结算"
            "EXIT_FORMALITIES" -> "离场手续"
            else -> displayText() ?: "离场申请"
        }
    }

    private fun String?.toProjectStatusName(): String {
        return when (this?.trim()) {
            "CONSTRUCTION", "IN_PROGRESS", "ACTIVE" -> "施工中"
            "PAUSED" -> "暂停"
            "COMPLETED", "FINISHED" -> "已完工"
            else -> displayText().orEmpty()
        }
    }

    private fun String?.displayText(): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun Double.toPlainText(): String {
        return if (this % 1.0 == 0.0) {
            toLong().toString()
        } else {
            toString()
        }
    }

    private fun <T> AjaxResp<List<T>>.requireList(): List<T> {
        requireSuccess(code, msg)
        return data.orEmpty()
    }

    private fun AjaxResp<*>.requireOk() {
        requireSuccess(code, msg)
    }

    private fun <T> AjaxResp<T>.requireData(): T {
        requireSuccess(code, msg)
        return checkNotNull(data) { msg ?: "接口未返回数据" }
    }
}
