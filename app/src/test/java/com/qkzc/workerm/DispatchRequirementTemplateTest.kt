package com.qkzc.workerm

import com.qkzc.workerm.ui.dispatch.DispatchRequirementTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DispatchRequirementTemplateTest {
    @Test
    fun repairTemplateUsesTitleLocationAndTypeSpecificSafety() {
        val template = DispatchRequirementTemplate.build(
            dispatchType = "REPAIR",
            title = "地下室照明维修",
            location = "B2配电间",
        )

        assertTrue(template.content.contains("地下室照明维修"))
        assertTrue(template.content.contains("B2配电间"))
        assertTrue(template.constructionRequirement.contains("维修前"))
        assertTrue(template.safetyNotice.contains("断电"))
    }

    @Test
    fun blankTitleAndLocationFallsBackToOperationalText() {
        val template = DispatchRequirementTemplate.build(
            dispatchType = "INSPECTION",
            title = "",
            location = "",
        )

        assertEquals("按派工类型完成现场巡检任务，记录问题并反馈处理结果。", template.content)
        assertTrue(template.constructionRequirement.contains("检查记录"))
        assertTrue(template.safetyNotice.contains("防护用品"))
    }
}
