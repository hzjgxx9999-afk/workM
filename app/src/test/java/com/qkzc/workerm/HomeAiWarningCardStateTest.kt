package com.qkzc.workerm

import com.qkzc.workerm.data.aiwarning.model.AiWarningSummary
import com.qkzc.workerm.ui.home.HomeAiWarningCardState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeAiWarningCardStateTest {

    @Test
    fun fromSummaryRendersMessageCenterWarningCounts() {
        val state = HomeAiWarningCardState.fromSummary(
            AiWarningSummary(
                totalCount = 3,
                pendingCount = 2,
                unreadCount = 1,
                highRiskCount = 0,
                maxRiskScore = 15,
            ),
        )

        assertEquals("15", state.scoreText)
        assertEquals("当前最高风险分", state.metricTitle)
        assertEquals("高风险 0 条", state.metricSubtitle)
        assertEquals(listOf("全部预警 3条", "待处理 2条", "未读 1条"), state.chips)
    }

    @Test
    fun errorStateDoesNotFallbackToOldStaticHomeValues() {
        val state = HomeAiWarningCardState.error()

        assertEquals("--", state.scoreText)
        assertEquals("预警加载失败", state.metricTitle)
        assertEquals("点击进入消息页查看", state.metricSubtitle)
        assertFalse(state.chips.contains("材料库存不足 1"))
        assertFalse(state.chips.contains("安全帽未佩戴 3"))
        assertFalse(state.chips.contains("考勤异常 2"))
    }
}
