package com.qkzc.workerm

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class DispatchCreateDeadlinePickerContractTest {

    @Test
    fun deadlineFieldIsPickerOnlyNotManualTextInput() {
        val layout = readMainFile("res/layout/activity_dispatch_create.xml")
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(layout)
        val deadlineInput = document.findElementById("@+id/deadline_input")

        assertEquals("false", deadlineInput.androidAttribute("focusable"))
        assertEquals("none", deadlineInput.androidAttribute("inputType"))
        assertEquals("false", deadlineInput.androidAttribute("cursorVisible"))
        assertTrue(
            "deadline input should guide user to choose a deadline",
            deadlineInput.androidAttribute("hint").orEmpty().contains("选择")
                || layout.readText().contains("选择截止时间"),
        )
    }

    @Test
    fun createActivityUsesSystemDateAndTimePickerDialogs() {
        val source = readMainFile(
            "java/com/qkzc/workerm/ui/dispatch/DispatchCreateActivity.kt",
        ).readText()

        assertTrue(source.contains("DatePickerDialog"))
        assertTrue(source.contains("TimePickerDialog"))
        assertTrue(source.contains("showDeadlinePicker"))
    }

    private fun readMainFile(relativePath: String): File {
        return sequenceOf(
            File("src/main/$relativePath"),
            File("app/src/main/$relativePath"),
        ).firstOrNull(File::isFile) ?: error("Missing main file: $relativePath")
    }

    private fun org.w3c.dom.Document.findElementById(id: String): Element {
        val nodes = getElementsByTagName("*")
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            val attributes = node.attributes ?: continue
            if (attributes.getNamedItem("android:id")?.nodeValue == id) {
                return node as Element
            }
        }
        error("Missing element id: $id")
    }

    private fun Element.androidAttribute(name: String): String? {
        return attributes.getNamedItem("android:$name")?.nodeValue
    }
}
