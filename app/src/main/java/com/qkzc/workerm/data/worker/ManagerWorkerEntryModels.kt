package com.qkzc.workerm.data.worker

import java.util.Locale

enum class EntryStepState(
    val statusText: String,
    val marker: String,
    val completed: Boolean,
) {
    NOT_STARTED("未开始", "·", false),
    IN_PROGRESS("进行中", "·", false),
    COMPLETED("已完成", "✓", true),
}

data class ManagerWorkerEntryStep(
    val code: String,
    val title: String,
    val rawStatus: String,
    val state: EntryStepState,
) {
    val statusText: String
        get() = state.statusText

    val displayText: String
        get() = "${state.marker}\n$title\n${state.statusText}"
}

fun ManagerWorker.entrySteps(): List<ManagerWorkerEntryStep> {
    return listOf(
        entryStep("identity", "身份认证", identityStatus),
        entryStep("safetyTraining", "安全培训", safetyTrainingStatus),
        entryStep("healthCheck", "健康检查", healthCheckStatus),
        entryStep("contract", "合同签署", entryContractStatus.ifBlank { contractStatus }),
        entryStep("insurance", "保险签署", insuranceStatus),
    )
}

fun ManagerWorker.entryStatusText(): String {
    val status = entryStatus.trim()
    if (status.isNotEmpty()) {
        return normalizeEntryState(status).statusText
    }
    val steps = entrySteps()
    return when {
        steps.all { it.state.completed } -> EntryStepState.COMPLETED.statusText
        steps.any { it.rawStatus.isNotBlank() && it.state != EntryStepState.NOT_STARTED } ->
            EntryStepState.IN_PROGRESS.statusText
        else -> EntryStepState.NOT_STARTED.statusText
    }
}

fun ManagerWorker.entryProgressText(): String {
    val completed = entrySteps().count { it.state.completed }
    return "入场流程（$completed/5 ${entryStatusText()}）"
}

private fun entryStep(code: String, title: String, status: String): ManagerWorkerEntryStep {
    return ManagerWorkerEntryStep(
        code = code,
        title = title,
        rawStatus = status,
        state = normalizeEntryState(status),
    )
}

private fun normalizeEntryState(status: String): EntryStepState {
    return when (status.trim().uppercase(Locale.ROOT)) {
        "", "0", "FALSE", "NOT_STARTED", "TODO", "WAITING" -> EntryStepState.NOT_STARTED
        "COMPLETED", "COMPLETE", "DONE", "PASS", "PASSED", "SIGNED", "SUCCESS", "1", "TRUE" ->
            EntryStepState.COMPLETED
        else -> EntryStepState.IN_PROGRESS
    }
}
