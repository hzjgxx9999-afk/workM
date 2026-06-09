package com.qkzc.workerm.data.worker

import java.util.Locale

enum class ManagerWorkerFilter(
    val title: String,
    val backendStatus: String?,
) {
    ALL("全部", null),
    ACTIVE("在场", "ACTIVE"),
    ENTERING("待入场", "ENTERING"),
    EXITED("已离场", "EXITED"),
}

data class ManagerWorkerMemberSummary(
    val totalWorkers: Int,
    val teamLeaderCount: Int,
    val activeWorkers: Int,
    val enteringWorkers: Int,
    val exitedWorkers: Int,
    val abnormalWorkers: Int,
)

fun List<ManagerWorker>.toMemberSummary(): ManagerWorkerMemberSummary {
    val normalized = map { it.normalizedBindStatus }
    return ManagerWorkerMemberSummary(
        totalWorkers = size,
        teamLeaderCount = map { it.leaderId }.filter { it > 0L }.distinct().size,
        activeWorkers = normalized.count { it == "ACTIVE" },
        enteringWorkers = normalized.count { it == "ENTERING" || it == "BOUND" },
        exitedWorkers = normalized.count { it == "EXITED" || it == "LEFT" },
        abnormalWorkers = normalized.count { it == "CANCELLED" || it == "REJECTED" || it == "ABNORMAL" },
    )
}

val ManagerWorker.normalizedBindStatus: String
    get() = bindStatus.ifBlank { entryStatus }.uppercase(Locale.getDefault())

fun ManagerWorker.memberStatusText(): String {
    return when (normalizedBindStatus) {
        "ACTIVE" -> "在场"
        "BOUND", "ENTERING" -> "待入场"
        "EXITED", "LEFT" -> "已离场"
        "CANCELLED" -> "已取消"
        "REJECTED" -> "已驳回"
        "COMPLETED" -> "已完成"
        else -> normalizedBindStatus.ifBlank { "--" }
    }
}
