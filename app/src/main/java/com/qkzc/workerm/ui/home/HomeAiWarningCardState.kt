package com.qkzc.workerm.ui.home

import com.qkzc.workerm.data.aiwarning.model.AiWarningSummary

data class HomeAiWarningCardState(
    val scoreText: String,
    val metricTitle: String,
    val metricSubtitle: String,
    val chips: List<String>,
) {
    companion object {
        fun loading(): HomeAiWarningCardState {
            return HomeAiWarningCardState(
                scoreText = "--",
                metricTitle = "正在加载预警",
                metricSubtitle = "请稍候",
                chips = listOf("全部预警 --", "待处理 --", "未读 --"),
            )
        }

        fun fromSummary(summary: AiWarningSummary): HomeAiWarningCardState {
            return HomeAiWarningCardState(
                scoreText = summary.maxRiskScore.toString(),
                metricTitle = "当前最高风险分",
                metricSubtitle = "高风险 ${summary.highRiskCount} 条",
                chips = listOf(
                    "全部预警 ${summary.totalCount}条",
                    "待处理 ${summary.pendingCount}条",
                    "未读 ${summary.unreadCount}条",
                ),
            )
        }

        fun error(): HomeAiWarningCardState {
            return HomeAiWarningCardState(
                scoreText = "--",
                metricTitle = "预警加载失败",
                metricSubtitle = "点击进入消息页查看",
                chips = listOf("全部预警 --", "待处理 --", "未读 --"),
            )
        }
    }
}
