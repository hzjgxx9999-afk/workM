package com.qkzc.workerm.data.dispatch

data class DispatchOrder(
    val id: Long = 0,
    val dispatchNo: String = "",
    val projectId: Long = 0,
    val projectName: String = "",
    val teamId: Long = 0,
    val teamName: String = "",
    val leaderId: Long = 0,
    val leaderName: String = "",
    val title: String = "",
    val dispatchType: String = "",
    val priority: String = "NORMAL",
    val content: String = "",
    val constructionRequirement: String? = null,
    val safetyNotice: String? = null,
    val acceptanceStandard: String? = null,
    val locationDesc: String = "",
    val deadlineTime: String = "",
    val status: String = "",
    val currentRound: Int = 1,
    val processCheckEnabled: Int = 0,
    val processCheckMode: String? = null,
    val processCheckDescription: String? = null,
    val processMinPhotoCount: Int = 0,
    val processRequireLocation: Int = 0,
    val managerSpotCheckEnabled: Int = 0,
    val beforePhotoRequired: Int = 0,
    val completionPhotoRequired: Int = 1,
    val completionMinPhotoCount: Int = 1,
    val leaderSummary: String? = null,
    val version: Int = 0,
    val overdue: Boolean = false,
)

data class DispatchDetail(
    val order: DispatchOrder = DispatchOrder(),
    val location: DispatchWorkOrderLocation? = null,
    val assignees: List<DispatchAssignee> = emptyList(),
    val attachments: List<DispatchAttachment> = emptyList(),
    val logs: List<DispatchLog> = emptyList(),
    val processNodes: List<DispatchProcessNode> = emptyList(),
    val processRecords: List<DispatchProcessRecord> = emptyList(),
)

/** 新版派工施工位置；经纬度统一为高德 GCJ-02。 */
data class DispatchWorkOrderLocation(
    val locationName: String = "",
    val longitude: Double? = null,
    val latitude: Double? = null,
    val building: String? = null,
    val floor: String? = null,
    val area: String? = null,
    val locationDescription: String? = null,
)

data class DispatchWorkOrderLocationInput(
    val locationName: String,
    val longitude: Double,
    val latitude: Double,
    val building: String? = null,
    val floor: String? = null,
    val area: String? = null,
    val locationDescription: String? = null,
)

data class DispatchAssignee(
    val id: Long = 0,
    val assigneeName: String = "",
    val roleType: String = "",
    val status: String = "",
    val resultRemark: String? = null,
    val version: Int = 0,
)

data class DispatchAttachment(
    val id: Long = 0,
    val fileName: String? = null,
    val fileUrl: String = "",
    val bizStage: String = "",
    val evidenceType: String? = null,
    val processNodeId: Long? = null,
    val processRecordId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
)

data class DispatchProcessNode(
    val id: Long = 0,
    val dispatchId: Long = 0,
    val roundNo: Int = 1,
    val nodeName: String = "",
    val nodeSort: Int = 0,
    val checkType: String = "NODE",
    val requiredPhotoCount: Int = 0,
    val requiredLocation: Int = 0,
    val reviewerRole: String = "TEAM_LEADER",
    val status: String = "WAIT_SUBMIT",
    val description: String? = null,
    val submitDeadline: String? = null,
)

data class DispatchProcessRecord(
    val id: Long = 0,
    val dispatchId: Long = 0,
    val processNodeId: Long = 0,
    val assigneeId: Long = 0,
    val submitUserName: String = "",
    val submitDescription: String = "",
    val submitTime: String? = null,
    val checkUserName: String? = null,
    val checkRole: String? = null,
    val checkTime: String? = null,
    val checkResult: String? = null,
    val checkComment: String? = null,
    val status: String = "WAIT_CHECK",
    val version: Int = 0,
)

data class DispatchAcceptanceDetail(
    val order: DispatchOrder = DispatchOrder(),
    val leaderSummary: String? = null,
    val summaryTime: String? = null,
    val workers: List<DispatchAcceptanceWorker> = emptyList(),
    val processChecks: List<DispatchAcceptanceProcessCheck> = emptyList(),
    val evidenceGroups: List<DispatchEvidenceGroup> = emptyList(),
    val spotChecks: List<DispatchSpotCheckSummary> = emptyList(),
    val permissions: DispatchAcceptancePermissions = DispatchAcceptancePermissions(),
)

data class DispatchAcceptanceWorker(
    val assigneeId: Long = 0,
    val userId: Long = 0,
    val name: String = "",
    val role: String = "WORKER",
    val status: String = "",
    val resultRemark: String? = null,
    val photoCount: Int = 0,
    val photoUrls: List<String> = emptyList(),
)

data class DispatchAcceptanceProcessCheck(
    val nodeId: Long = 0,
    val nodeName: String = "",
    val status: String = "",
    val reviewerName: String? = null,
    val checkTime: String? = null,
    val photoCount: Int = 0,
    val photoUrls: List<String> = emptyList(),
)

data class DispatchEvidenceGroup(
    val type: String = "",
    val title: String = "",
    val count: Int = 0,
    val previewUrls: List<String> = emptyList(),
)

data class DispatchSpotCheckSummary(
    val nodeId: Long = 0,
    val nodeName: String = "",
    val status: String = "WAIT_SUBMIT",
    val latestRecordId: Long? = null,
    val submitDeadline: String? = null,
    val overdue: Boolean = false,
)

data class DispatchAcceptancePermissions(
    val canApprove: Boolean = false,
    val canReject: Boolean = false,
    val canCreateSpotCheck: Boolean = false,
    val canReassign: Boolean = false,
    val canCancel: Boolean = false,
    val approvalBlockedReason: String? = null,
)

data class DispatchSpotCheckDetail(
    val order: DispatchOrder = DispatchOrder(),
    val node: DispatchProcessNode = DispatchProcessNode(),
    val latestSubmission: DispatchSpotCheckSubmission? = null,
    val photos: List<DispatchSpotCheckPhoto> = emptyList(),
    val linkedProcessChecks: List<DispatchLinkedProcessCheck> = emptyList(),
    val operations: List<DispatchSpotCheckOperation> = emptyList(),
    val overdue: Boolean = false,
    val canSubmit: Boolean = false,
    val canReview: Boolean = false,
    val canCancel: Boolean = false,
)

data class DispatchSpotCheckSubmission(
    val recordId: Long = 0,
    val submitterName: String = "",
    val description: String = "",
    val status: String = "",
    val version: Int = 0,
    val submitTime: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val coordType: String? = null,
    val checkUserName: String? = null,
    val checkResult: String? = null,
    val checkComment: String? = null,
    val checkTime: String? = null,
)

data class DispatchSpotCheckPhoto(
    val attachmentId: Long = 0,
    val fileName: String? = null,
    val previewUrl: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val captureTime: String? = null,
)

data class DispatchLinkedProcessCheck(
    val nodeId: Long = 0,
    val nodeName: String = "",
    val status: String = "",
    val reviewerName: String? = null,
    val checkTime: String? = null,
)

data class DispatchSpotCheckOperation(
    val action: String = "",
    val operatorName: String = "",
    val operatorRole: String = "",
    val remark: String? = null,
    val createTime: String? = null,
)

data class DispatchLog(
    val id: Long = 0,
    val operatorName: String = "",
    val action: String = "",
    val remark: String? = null,
    val createTime: String? = null,
)

data class DispatchTableResp<T>(
    val code: Int = 0,
    val msg: String? = null,
    val total: Long = 0,
    val rows: List<T> = emptyList(),
)

data class DispatchProcessNodeInput(
    val nodeName: String,
    val nodeSort: Int,
    val checkType: String = "NODE",
    val requiredPhotoCount: Int = 2,
    val requiredLocation: Int = 0,
    val reviewerRole: String = "TEAM_LEADER",
    val description: String? = null,
)

data class DispatchCreateReq(
    val projectId: Long,
    val teamId: Long,
    val leaderId: Long,
    val title: String,
    val dispatchType: String,
    val priority: String,
    val content: String,
    val locationDesc: String?,
    val location: DispatchWorkOrderLocationInput? = null,
    val deadlineTime: String,
    val constructionRequirement: String? = null,
    val safetyNotice: String? = null,
    val acceptanceStandard: String? = null,
    val processCheckEnabled: Boolean = false,
    val processCheckMode: String? = null,
    val processCheckDescription: String? = null,
    val processNodes: List<DispatchProcessNodeInput> = emptyList(),
    val processMinPhotoCount: Int = 0,
    val processRequireLocation: Boolean = false,
    val managerSpotCheckEnabled: Boolean = false,
    val beforePhotoRequired: Boolean = false,
    val completionPhotoRequired: Boolean = true,
    val completionMinPhotoCount: Int = 1,
)

data class DispatchActionReq(val version: Int, val remark: String? = null)
data class DispatchProcessReviewReq(val result: String, val comment: String? = null, val version: Int? = null)
data class DispatchManualProcessNodeReq(
    val nodeName: String,
    val nodeSort: Int = 999,
    val checkType: String = "MANUAL",
    val requiredPhotoCount: Int = 2,
    val requiredLocation: Int = 0,
    val reviewerRole: String = "PROJECT_MANAGER",
    val description: String? = null,
    val submitDeadline: String? = null,
)
data class DispatchReassignReq(val teamId: Long, val leaderId: Long, val version: Int)

object DispatchStatusPresentation {
    fun summaryText(summary: String?): String =
        summary.orEmpty().ifBlank { "班组暂未提交汇总" }

    fun label(status: String?, overdue: Boolean = false): String {
        if (overdue) return "已超时"
        return when (status) {
            "PENDING_DELEGATION", "PENDING_REASSIGNMENT" -> "待分派"
            "IN_PROGRESS" -> "处理中"
            "PENDING_PROCESS_CHECK" -> "待过程检查"
            "PROCESS_REWORK" -> "过程整改"
            "PENDING_WORKER_RESULT_REVIEW" -> "待结果审核"
            "PENDING_LEADER_SUMMARY" -> "待汇总"
            "PENDING_MANAGER_ACCEPTANCE" -> "待验收"
            "COMPLETED" -> "已完成"
            "CANCELLED" -> "已取消"
            "WAIT_SUBMIT" -> "待提交"
            "WAIT_CHECK" -> "待审核"
            "PASSED" -> "已通过"
            "REJECTED" -> "已驳回"
            "SUBMITTED" -> "已提交"
            "REVIEWED" -> "已审核"
            else -> status.orEmpty()
        }
    }

    fun canReassign(status: String?) = status != null && status !in setOf("COMPLETED", "CANCELLED")
    fun canCancel(status: String?) = status != null && status !in setOf("COMPLETED", "CANCELLED")
}
