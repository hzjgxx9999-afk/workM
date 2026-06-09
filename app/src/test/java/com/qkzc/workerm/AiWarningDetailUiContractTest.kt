package com.qkzc.workerm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AiWarningDetailUiContractTest {

    @Test
    fun managerAiWarningDetailUsesCustomViewWithPhotoPreview() {
        val fragment = File("src/main/java/com/qkzc/workerm/ui/message/MessageFragment.kt").readText()

        assertFalse(fragment.contains(".setMessage(detail.toDialogMessage())"))
        assertTrue(fragment.contains("ImageView"))
        assertTrue(fragment.contains("loadWarningPhoto"))
        assertTrue(fragment.contains("detail.item.photoUrl"))
        assertTrue(fragment.contains("巡查结论"))
        assertTrue(fragment.contains("URL(photoUrl).openStream()"))
        assertFalse(fragment.contains("ApiClient.okHttpClient.newCall"))
    }
}
