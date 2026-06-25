package com.qkzc.workerm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MaterialWarningUiContractTest {

    @Test
    fun materialHomeNotificationButtonOpensWarningListInsteadOfToast() {
        val source = File("src/main/java/com/qkzc/workerm/ui/material/MaterialHomeActivity.kt").readText()

        assertTrue(source.contains("MaterialWarningActivity"))
        assertFalse(source.contains("暂无新的材料库存预警"))
    }

    @Test
    fun materialWarningActivityIsRegisteredInManifest() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains(".ui.material.MaterialWarningActivity"))
    }
}
