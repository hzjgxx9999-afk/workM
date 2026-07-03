package com.qkzc.workerm.ui.worker

import com.qkzc.workerm.data.worker.WorkerQrVerifyResult

enum class WorkerQrVerifyUiTone {
    WAITING,
    PASS,
    FAIL,
    EXPIRED,
}

data class WorkerQrVerifyUiState(
    val tone: WorkerQrVerifyUiTone,
    val statusTitle: String,
    val statusSubtitle: String,
    val workerName: String,
    val workType: String,
    val teamLine: String,
    val presenceText: String,
    val showFailureReason: Boolean,
    val failureReason: String,
    val projectName: String,
    val leaderName: String,
    val mobile: String,
    val idCardNo: String,
    val entryValue: String,
    val entryHint: String,
    val trainingValue: String,
    val trainingHint: String,
    val healthValue: String,
    val healthHint: String,
    val contractValue: String,
    val contractHint: String,
    val footerText: String,
)

object WorkerQrVerifyUiMapper {

    fun waiting(): WorkerQrVerifyUiState {
        return WorkerQrVerifyUiState(
            tone = WorkerQrVerifyUiTone.WAITING,
            statusTitle = "对准二维码",
            statusSubtitle = "识别后自动核验",
            workerName = "等待扫码",
            workType = "请扫描工人端个人二维码",
            teamLine = "管理端按当前项目范围核验",
            presenceText = "待扫",
            showFailureReason = false,
            failureReason = "",
            projectName = "--",
            leaderName = "--",
            mobile = "--",
            idCardNo = "--",
            entryValue = "等待扫码",
            entryHint = "扫描工人二维码",
            trainingValue = "当前项目",
            trainingHint = "按项目权限过滤",
            healthValue = "自动核验",
            healthHint = "无需手动输入",
            contractValue = "清晰说明",
            contractHint = "过期/离场/无权限",
            footerText = "识别成功后自动核验",
        )
    }

    fun error(message: String): WorkerQrVerifyUiState {
        return waiting().copy(
            tone = WorkerQrVerifyUiTone.FAIL,
            statusTitle = "核验失败",
            statusSubtitle = message.ifBlank { "请确认二维码和项目范围" },
            workerName = "暂无工人",
            workType = message.ifBlank { "工人信息加载失败" },
            teamLine = "请确认二维码和项目范围",
            presenceText = "异常",
            showFailureReason = true,
            failureReason = message.ifBlank { "工人信息加载失败，请重新扫码。" },
            entryValue = "失败",
            entryHint = "请重新扫码",
            footerText = "点击继续扫码 >",
        )
    }

    fun fromResult(result: WorkerQrVerifyResult, scanTime: String): WorkerQrVerifyUiState {
        val exited = result.message.contains("离场") ||
            result.exitStatus.equals("COMPLETED", ignoreCase = true) ||
            result.exitStatus.contains("离场")
        val ticketExpired = result.status.equals("TICKET_EXPIRED", ignoreCase = true) ||
            result.status.contains("EXPIRED", ignoreCase = true) ||
            result.message.contains("过期")
        val ticketInvalid = !result.ticketValid
        val expired = ticketExpired && !exited
        val moduleIncomplete = hasBlockingIncompleteStatus(
            result.entryStatus,
            result.safetyTrainingStatus,
            result.healthCheckStatus,
            result.contractStatus,
        )
        val pass = result.pass && !exited && !moduleIncomplete
        val tone = when {
            pass -> WorkerQrVerifyUiTone.PASS
            expired -> WorkerQrVerifyUiTone.EXPIRED
            else -> WorkerQrVerifyUiTone.FAIL
        }
        val failureReason = when {
            pass -> ""
            expired -> "二维码已过期，请让工人刷新二维码后重新扫码。"
            exited -> "工人已完成离场，不能作为当前在场人员核验通过。"
            moduleIncomplete -> "当前项目入场资料未完成，不能作为在场人员核验通过。"
            ticketInvalid -> result.message.ifBlank { "二维码无效，请让工人刷新二维码后重新扫码。" }
            result.message.isNotBlank() -> result.message
            else -> "核验未通过，请确认工人状态和项目范围。"
        }

        return WorkerQrVerifyUiState(
            tone = tone,
            statusTitle = when (tone) {
                WorkerQrVerifyUiTone.PASS -> "核验通过"
                WorkerQrVerifyUiTone.EXPIRED -> "二维码过期"
                WorkerQrVerifyUiTone.FAIL -> "核验失败"
                WorkerQrVerifyUiTone.WAITING -> "对准二维码"
            },
            statusSubtitle = when {
                pass -> "允许进入项目现场"
                expired -> "请让工人刷新二维码"
                exited -> "该工人已完成离场"
                moduleIncomplete -> "当前项目入场资料未完成"
                ticketInvalid -> result.message.ifBlank { "二维码无效，请重新扫码" }
                result.message.isNotBlank() -> result.message
                else -> "请确认工人状态"
            },
            workerName = result.workerName.ifBlank { "暂无工人" },
            workType = result.workTypeName.ifBlank { result.message.ifBlank { "未配置工种" } },
            teamLine = "所属班组：${result.teamName.ifBlank { result.leaderName.ifBlank { "未绑定班组" } }}",
            presenceText = when {
                pass -> "在场"
                exited -> "离场"
                expired -> "过期"
                else -> "异常"
            },
            showFailureReason = !pass,
            failureReason = failureReason,
            projectName = result.projectName.ifBlank { "--" },
            leaderName = result.leaderName.ifBlank { "--" },
            mobile = maskMobile(result.mobile),
            idCardNo = maskIdCard(result.idCardNo),
            entryValue = if (exited) "已离场" else statusText(result.entryStatus),
            entryHint = when {
                exited -> "已离场不放行"
                expired -> "二维码无效"
                else -> statusHint(result.entryStatus, completedText = "流程已完成")
            },
            trainingValue = statusText(result.safetyTrainingStatus),
            trainingHint = statusHint(result.safetyTrainingStatus, completedText = "已满足要求"),
            healthValue = statusText(result.healthCheckStatus),
            healthHint = statusHint(result.healthCheckStatus, completedText = "合格有效"),
            contractValue = statusText(result.contractStatus.ifBlank { result.signedTime }),
            contractHint = contractHint(result.contractStatus, result.signedTime),
            footerText = "扫描时间：$scanTime        点击继续扫码 >",
        )
    }

    private fun statusText(status: String): String {
        val value = status.trim()
        if (value.isBlank()) return "--"
        return when (value.uppercase()) {
            "COMPLETED", "DONE", "FINISHED", "1" -> "已完成"
            "NOT_STARTED", "0" -> "未开始"
            "IN_PROGRESS", "PROCESSING" -> "进行中"
            "ACTIVE" -> "在场"
            "EXITED", "OUT" -> "已离场"
            else -> value
        }
    }

    private fun statusHint(status: String, completedText: String): String {
        val value = status.trim()
        if (value.isBlank()) return "暂无记录"
        return when (value.uppercase()) {
            "COMPLETED", "DONE", "FINISHED", "1" -> completedText
            "NOT_STARTED", "0" -> "尚未开始"
            "IN_PROGRESS", "PROCESSING", "PENDING" -> "办理中"
            "REJECTED", "FAILED", "FAIL" -> "未通过"
            "EXITED", "OUT" -> "已离场"
            else -> "当前状态"
        }
    }

    private fun contractHint(status: String, signedTime: String): String {
        if (status.isBlank() && signedTime.isNotBlank()) return "已签署"
        val value = status.trim()
        if (value.isBlank()) return "暂无记录"
        return when (value.uppercase()) {
            "COMPLETED", "DONE", "FINISHED", "1" -> "已签署"
            "NOT_STARTED", "0" -> "尚未签署"
            "IN_PROGRESS", "PROCESSING", "PENDING" -> "签署中"
            "REJECTED", "FAILED", "FAIL" -> "未通过"
            else -> "当前状态"
        }
    }

    private fun hasBlockingIncompleteStatus(vararg statuses: String): Boolean {
        return statuses.any { status ->
            when (status.trim().uppercase()) {
                "NOT_STARTED", "0", "未开始",
                "IN_PROGRESS", "PROCESSING", "PENDING", "进行中", "办理中",
                "REJECTED", "FAILED", "FAIL", "未通过",
                "EXITED", "OUT", "已离场" -> true
                else -> false
            }
        }
    }

    private fun maskMobile(raw: String): String {
        val mobile = raw.trim()
        if (mobile.isBlank()) return "--"
        if (mobile.contains("*")) return mobile
        return if (mobile.length >= 7) {
            mobile.take(3) + "****" + mobile.takeLast(4)
        } else {
            mobile
        }
    }

    private fun maskIdCard(raw: String): String {
        val idCardNo = raw.trim()
        if (idCardNo.isBlank()) return "--"
        if (idCardNo.contains("*")) return idCardNo
        return if (idCardNo.length >= 8) {
            idCardNo.take(3) + "***********" + idCardNo.takeLast(4)
        } else {
            idCardNo
        }
    }
}
