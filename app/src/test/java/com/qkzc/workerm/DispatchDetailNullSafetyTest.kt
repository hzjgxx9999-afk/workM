package com.qkzc.workerm

import com.google.gson.Gson
import com.qkzc.workerm.data.dispatch.DispatchOrder
import com.qkzc.workerm.data.dispatch.DispatchStatusPresentation
import org.junit.Assert.assertEquals
import org.junit.Test

class DispatchDetailNullSafetyTest {

    @Test
    fun nullLeaderSummaryFromBackendUsesPendingSummaryText() {
        val order = Gson().fromJson(
            """{"leaderSummary":null}""",
            DispatchOrder::class.java,
        )

        assertEquals(
            "班组暂未提交汇总",
            DispatchStatusPresentation.summaryText(order.leaderSummary),
        )
    }
}
