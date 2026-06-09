package com.qkzc.workerm.data.network

import com.google.gson.annotations.SerializedName

data class LoginReq(
    val mobile: String,
    val password: String,
)

data class AjaxTokenResp(
    val code: Int,
    val msg: String?,
    val token: String?,
)

data class AjaxProfileResp(
    val code: Int,
    val msg: String?,
    val appUserId: Long?,
    val mobile: String?,
    val userType: String?,
    val clientType: String?,
)

data class AjaxResp<T>(
    val code: Int,
    val msg: String?,
    val data: T?,
)

data class ManagerProjectVo(
    val projectId: Long? = null,
    val projectCode: String? = null,
    val projectName: String? = null,
    val projectAddress: String? = null,
    val contractAmount: Double? = null,
    val employerUnit: String? = null,
    val subcontractorUnit: String? = null,
    val financeManagerName: String? = null,
    val safetyManagerName: String? = null,
    val projectManagerName: String? = null,
    val projectStatus: String? = null,
    val progressPercent: Int? = null,
    val startDate: String? = null,
    val plannedFinishDate: String? = null,
    @SerializedName(
        value = "coverImageUrl",
        alternate = ["cover_image_url", "coverUrl", "coverImgUrl", "imageUrl"],
    )
    val coverImageUrl: String? = null,
    val daysToFinish: Long? = null,
    val workerCount: Int? = null,
    val unhandledRiskCount: Long? = null,
    val highRiskCount: Long? = null,
    val teamLeaders: List<ManagerTeamLeaderVo>? = null,
    val recentFiles: List<ManagerProjectFileVo>? = null,
)

data class ManagerProjectFileVo(
    val fileId: Long? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val fileSize: Long? = null,
    val category: String? = null,
    val createTime: String? = null,
)

data class ManagerTeamLeaderVo(
    val relationId: Long? = null,
    val projectId: Long? = null,
    val projectName: String? = null,
    val leaderId: Long? = null,
    val leaderName: String? = null,
)

data class ManagerProjectTeamVo(
    val teamId: Long? = null,
    val relationId: Long? = null,
    val teamName: String? = null,
    val leaderId: Long? = null,
    val leaderName: String? = null,
    val workTypeId: Long? = null,
    val workTypeName: String? = null,
    val status: String? = null,
    val remark: String? = null,
)

data class ManagerLeaderOptionVo(
    val leaderId: Long? = null,
    val leaderName: String? = null,
    val mobile: String? = null,
)

data class ManagerWorkTypeOptionVo(
    val workTypeId: Long? = null,
    val workTypeName: String? = null,
)

data class ManagerProjectTeamSaveReq(
    val teamName: String,
    val leaderId: Long,
    val workTypeId: Long? = null,
    val remark: String? = null,
)

data class ManagerProjectTeamStatusReq(
    val status: String,
)

data class ManageInviteCodeVo(
    val id: Long? = null,
    val inviteCode: String? = null,
    val projectId: Long? = null,
    val projectName: String? = null,
    val leaderId: Long? = null,
    val leaderName: String? = null,
    val teamId: Long? = null,
    val teamName: String? = null,
    val expireTime: String? = null,
    val maxUseCount: Int? = null,
    val usedCount: Int? = null,
    val status: String? = null,
    val remark: String? = null,
    val registerDeepLink: String? = null,
    val qrContent: String? = null,
)

data class ManageInviteCodeCreateReq(
    val projectId: Long,
    val leaderId: Long,
    val teamId: Long,
    val expireTime: String? = null,
    val maxUseCount: Int? = null,
    val remark: String? = null,
)

data class ManageInviteCodeStatusReq(
    val status: String,
)

data class ManagerHomeOverviewVo(
    val projectCount: Int? = null,
    val workerCount: Int? = null,
    val currentProjectId: Long? = null,
    val projects: List<ManagerProjectVo>? = null,
    val riskSummary: ManagerRiskSummaryVo? = null,
)

data class ManagerRiskSummaryVo(
    val unhandledRiskCount: Long? = null,
    val highRiskCount: Long? = null,
    val unreadRiskCount: Long? = null,
)

data class ManagerWorkerVo(
    val relationId: Long? = null,
    val entryId: Long? = null,
    val workerUserId: Long? = null,
    val realName: String? = null,
    val mobile: String? = null,
    val idCardNo: String? = null,
    val userStatus: Long? = null,
    val workTypeName: String? = null,
    val workTypeId: Long? = null,
    val projectId: Long? = null,
    val projectName: String? = null,
    val leaderId: Long? = null,
    val leaderName: String? = null,
    val teamId: Long? = null,
    val teamName: String? = null,
    val bindStatus: String? = null,
    val bindTime: String? = null,
    val enterTime: String? = null,
    val exitTime: String? = null,
    val entryStatus: String? = null,
    val identityStatus: String? = null,
    val safetyTrainingStatus: String? = null,
    val healthCheckStatus: String? = null,
    val entryContractStatus: String? = null,
    val insuranceStatus: String? = null,
    val signed: Long? = null,
    val signedTime: String? = null,
    val contractStatus: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null,
)

data class ManagerWorkerScanReq(
    val projectId: Long,
    @SerializedName("workerId")
    val workerUserId: Long? = null,
    val ticket: String? = null,
)

data class AuditListReq(
    val projectId: Long? = null,
    val status: String? = ApprovalApiConstants.STATUS_PENDING_MANAGER,
    val workerId: Long? = null,
)

data class MaterialListReq(
    val projectId: Long? = null,
    val status: String? = ApprovalApiConstants.STATUS_PENDING_MANAGER,
    val workerId: Long? = null,
    val itemName: String? = null,
)

data class AdvanceDetailReq(
    val requestId: Long,
)

data class MaterialDetailReq(
    val requestId: Long,
)

data class ExceptionDetailReq(
    val exceptionId: Long,
)

data class ExitListReq(
    val projectId: Long? = null,
    val status: String? = ApprovalApiConstants.STATUS_PENDING_MANAGER,
    val workerId: Long? = null,
    val requestType: String? = null,
)

data class ExitDetailReq(
    val requestId: Long,
)

data class AdvanceAuditReq(
    val requestId: Long,
    val action: String,
    val auditRemark: String,
)

data class MaterialAuditReq(
    val requestId: Long,
    val action: String,
    val auditRemark: String,
)

data class ExceptionAuditReq(
    val exceptionId: Long,
    val action: String,
    val auditRemark: String,
)

data class ExitAuditReq(
    val requestId: Long,
    val action: String,
    val auditRemark: String,
)

data class AiWarningListReq(
    val projectId: Long? = null,
    val readFlag: Int? = null,
    val handleStatus: String? = null,
    val riskLevel: String? = null,
    val pageNum: Int = 1,
    val pageSize: Int = 20,
)

data class AiWarningDetailReq(
    val warningId: Long,
)

data class AiWarningReadReq(
    val warningId: Long,
)

data class AiWarningHandleReq(
    val warningId: Long,
    val handleStatus: String,
)

data class AdvanceRequestVo(
    val id: Long,
    val userId: Long?,
    val realName: String? = null,
    val mobile: String? = null,
    val projectId: Long?,
    val projectName: String? = null,
    val projectAddress: String? = null,
    val contractorUnit: String? = null,
    val projectStatus: String? = null,
    val leaderId: Long?,
    val leaderName: String? = null,
    val teamName: String? = null,
    val amount: Double?,
    val reason: String?,
    val status: String?,
    val statusLabel: String?,
    val currentNodeLabel: String?,
    val leaderAuditStatus: String?,
    val leaderAuditStatusLabel: String?,
    val managerAuditStatus: String?,
    val managerAuditStatusLabel: String?,
    val leaderAuditUserId: Long?,
    val leaderAuditTime: String?,
    val leaderAuditRemark: String?,
)

data class MaterialRequestVo(
    val id: Long,
    val userId: Long?,
    val realName: String? = null,
    val mobile: String? = null,
    val projectId: Long?,
    val projectName: String? = null,
    val projectAddress: String? = null,
    val contractorUnit: String? = null,
    val projectStatus: String? = null,
    val leaderId: Long?,
    val leaderName: String? = null,
    val teamName: String? = null,
    val itemName: String?,
    val quantity: Double?,
    val unit: String?,
    val reason: String?,
    val status: String?,
    val statusLabel: String?,
    val currentNodeLabel: String?,
    val leaderAuditStatus: String?,
    val leaderAuditStatusLabel: String?,
    val managerAuditStatus: String?,
    val managerAuditStatusLabel: String?,
)

data class AttendanceExceptionVo(
    val id: Long,
    val userId: Long?,
    val realName: String? = null,
    val mobile: String? = null,
    val projectId: Long?,
    val projectName: String? = null,
    val projectAddress: String? = null,
    val contractorUnit: String? = null,
    val projectStatus: String? = null,
    val leaderId: Long?,
    val leaderName: String? = null,
    val teamName: String? = null,
    val attendanceId: Long?,
    val exceptionType: String?,
    val exceptionTypeLabel: String?,
    val reason: String?,
    val workDate: String?,
    val fixCheckInTime: String?,
    val fixCheckOutTime: String?,
    val status: String?,
    val statusLabel: String?,
    val currentNodeLabel: String?,
    val leaderAuditStatus: String?,
    val leaderAuditStatusLabel: String?,
    val managerAuditStatus: String?,
    val managerAuditStatusLabel: String?,
)

data class ExitRequestVo(
    val id: Long,
    val exitProcessId: Long? = null,
    val entryId: Long? = null,
    val projectId: Long? = null,
    val projectName: String? = null,
    val projectAddress: String? = null,
    val contractorUnit: String? = null,
    val projectStatus: String? = null,
    val leaderId: Long? = null,
    val leaderName: String? = null,
    val teamName: String? = null,
    val requestType: String? = null,
    val requestTypeLabel: String? = null,
    val requestStatus: String? = null,
    val statusLabel: String? = null,
    val applicantId: Long? = null,
    val realName: String? = null,
    val mobile: String? = null,
    val approverId: Long? = null,
    val approverName: String? = null,
    val payloadJson: String? = null,
    val attachmentsJson: String? = null,
    val applyTime: String? = null,
    val approveTime: String? = null,
    val approveRemark: String? = null,
    val leaderAuditStatus: String? = null,
    val leaderAuditUserId: Long? = null,
    val leaderAuditTime: String? = null,
    val leaderAuditRemark: String? = null,
    val managerAuditStatus: String? = null,
    val managerAuditUserId: Long? = null,
    val managerAuditTime: String? = null,
    val managerAuditRemark: String? = null,
)

data class AiWarningPageVo(
    val total: Long? = null,
    val pageNum: Int? = null,
    val pageSize: Int? = null,
    val pages: Int? = null,
    val hasMore: Boolean? = null,
    val rows: List<AiWarningVo>? = null,
)

data class AiWarningVo(
    val warningId: Long? = null,
    val recordId: Long? = null,
    val receiverRole: String? = null,
    val workerUserId: Long? = null,
    val workerName: String? = null,
    val projectId: Long? = null,
    val projectName: String? = null,
    val leaderId: Long? = null,
    val leaderName: String? = null,
    val sceneName: String? = null,
    val inspectionPoint: String? = null,
    val workType: String? = null,
    val photoUrl: String? = null,
    val riskLevel: String? = null,
    val riskScore: Int? = null,
    val title: String? = null,
    val content: String? = null,
    val summary: String? = null,
    val readFlag: Boolean? = null,
    val handleStatus: String? = null,
    val createTime: String? = null,
    val readTime: String? = null,
    val handleTime: String? = null,
    val captureTime: String? = null,
    val aiModel: String? = null,
    val hazards: List<AiWarningHazardVo>? = null,
    val needManualReview: Boolean? = null,
    val manualReviewReason: String? = null,
    val status: String? = null,
    val errorMsg: String? = null,
)

data class AiWarningHazardVo(
    val code: String? = null,
    val name: String? = null,
    val level: String? = null,
    val confidence: Double? = null,
    val evidence: String? = null,
    val advice: String? = null,
)

object ApprovalApiConstants {
    const val ACTION_APPROVE = "APPROVE"
    const val ACTION_REJECT = "REJECT"
    const val STATUS_PENDING_MANAGER = "PENDING_MANAGER"
    const val STATUS_ALL = "ALL"
    const val USER_TYPE_PROJECT_MANAGER = "PROJECT_MANAGER"
}
