package com.qkzc.workerm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeAiWarningCardUiContractTest {

    @Test
    fun homeAiWarningCardExposesBindableRealSummaryViewsAndRemovesStaticFakeValues() {
        val layout = File("src/main/res/layout/fragment_home.xml").readText()

        assertTrue(layout.contains("@+id/home_ai_warning_score_text"))
        assertTrue(layout.contains("@+id/home_ai_warning_metric_title_text"))
        assertTrue(layout.contains("@+id/home_ai_warning_metric_subtitle_text"))
        assertTrue(layout.contains("@+id/home_ai_warning_total_chip"))
        assertTrue(layout.contains("@+id/home_ai_warning_pending_chip"))
        assertTrue(layout.contains("@+id/home_ai_warning_unread_chip"))

        assertFalse(layout.contains("android:text=\"72\""))
        assertFalse(layout.contains("今日异常事件"))
        assertFalse(layout.contains("材料库存不足 1"))
        assertFalse(layout.contains("安全帽未佩戴 3"))
        assertFalse(layout.contains("考勤异常 2"))
    }
}
