package com.qkzc.workerm.data.aiwarning.model

object AiWarningHandleStatus {
    const val PENDING = "PENDING"
    const val HANDLED = "HANDLED"
    const val IGNORED = "IGNORED"
}

object AiWarningRiskLevel {
    const val LOW = "LOW"
    const val MEDIUM = "MEDIUM"
    const val HIGH = "HIGH"
}

data class AiWarningPage(
    val total: Long,
    val unreadCount: Int,
    val pageNum: Int,
    val pageSize: Int,
    val pages: Int,
    val hasMore: Boolean,
    val rows: List<AiWarningItem>,
)

data class AiWarningItem(
    val warningId: Long,
    val recordId: Long?,
    val receiverRole: String,
    val workerName: String,
    val projectName: String,
    val leaderName: String,
    val sceneName: String,
    val inspectionPoint: String,
    val workType: String,
    val photoUrl: String?,
    val riskLevel: String,
    val riskScore: Int?,
    val title: String,
    val content: String,
    val summary: String,
    val readFlag: Boolean,
    val handleStatus: String,
    val createTime: String,
) {
    val riskLevelText: String
        get() = when (riskLevel) {
            AiWarningRiskLevel.HIGH -> "严重"
            AiWarningRiskLevel.MEDIUM -> "警示"
            AiWarningRiskLevel.LOW -> "提示"
            else -> riskLevel.ifBlank { "未知" }
        }

    val handleStatusText: String
        get() = when (handleStatus) {
            AiWarningHandleStatus.HANDLED -> "已处理"
            AiWarningHandleStatus.IGNORED -> "已忽略"
            else -> "待处理"
        }
}

data class AiWarningDetail(
    val item: AiWarningItem,
    val captureTime: String,
    val aiModel: String,
    val hazards: List<AiWarningHazard>,
    val needManualReview: Boolean,
    val manualReviewReason: String,
    val status: String,
    val errorMsg: String?,
)

data class AiWarningHazard(
    val code: String,
    val name: String,
    val level: String,
    val confidence: Double?,
    val evidence: String,
    val advice: String,
)
