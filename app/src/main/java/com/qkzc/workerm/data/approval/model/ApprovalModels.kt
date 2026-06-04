package com.qkzc.workerm.data.approval.model

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
}

enum class ApprovalFilter {
    PENDING,
    PROCESSED,
}

enum class ApprovalCategory {
    ADVANCE,
    MATERIAL,
    EXCEPTION,
}

data class ApprovalItem(
    val id: Long,
    val category: ApprovalCategory,
    val formNo: String,
    val typeName: String,
    val title: String,
    val applicantName: String,
    val projectName: String,
    val submittedAt: String,
    val currentNodeName: String,
    val reason: String,
    val status: ApprovalStatus,
    val statusName: String? = null,
    val reviewResultName: String? = null,
    val reviewerName: String? = null,
    val reviewedAt: String? = null,
    val reviewRemark: String? = null,
)

data class ApprovalSummary(
    val pendingCount: Int,
    val processedCount: Int,
    val rejectedCount: Int,
)
