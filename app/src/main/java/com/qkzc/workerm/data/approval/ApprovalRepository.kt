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

        return advances + materials + exceptions
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
            applicantName = userId?.let { "工人 $it" }.orEmpty(),
            projectName = projectId?.let { "项目 $it" }.orEmpty(),
            submittedAt = leaderAuditTime.orEmpty(),
            currentNodeName = currentNodeLabel.displayText() ?: "项目经理终审",
            reason = reason.orEmpty(),
            status = approvalStatus,
            statusName = statusLabel.displayText() ?: approvalStatus.toDisplayName(),
            reviewResultName = managerAuditStatusLabel.displayText()
                ?: leaderAuditStatusLabel.displayText()
                ?: statusLabel.displayText()
                ?: approvalStatus.toDisplayName(),
            reviewerName = leaderAuditUserId?.let { "班组长 $it" },
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
            applicantName = userId?.let { "工人 $it" }.orEmpty(),
            projectName = projectId?.let { "项目 $it" }.orEmpty(),
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
            applicantName = userId?.let { "工人 $it" }.orEmpty(),
            projectName = projectId?.let { "项目 $it" }.orEmpty(),
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
}
