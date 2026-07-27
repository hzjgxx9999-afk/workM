package com.qkzc.workerm

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DispatchProjectFilterUiContractTest {

    @Test
    fun dispatchListExposesProjectFilterAndBottomCreateAction() {
        val layout = mainFile("res/layout/activity_dispatch_list.xml").readText()
        val source = mainFile("java/com/qkzc/workerm/ui/dispatch/DispatchListActivity.kt").readText()

        assertTrue(layout.contains("@+id/project_filter_button"))
        assertTrue(layout.contains("@+id/status_spinner"))
        assertTrue(layout.contains("@+id/create_fab"))
        assertTrue(layout.contains("ExtendedFloatingActionButton"))
        assertFalse(layout.contains("@+id/create_button"))
        assertTrue(source.contains("loadProjectOptions"))
        assertTrue(source.contains("showProjectSelector"))
        assertTrue(source.contains("projectId = projectId"))
        assertTrue(source.contains("DispatchCreateActivity.EXTRA_PROJECT_ID"))
        assertTrue(source.contains("loadJob?.cancel()"))
        assertTrue(source.contains("selectedProjectId != projectId"))
    }

    @Test
    fun homeDispatchPreviewUsesProjectScopeAndFiveRows() {
        val source = mainFile("java/com/qkzc/workerm/ui/home/HomeFragment.kt").readText()

        assertTrue(source.contains("currentHomeDispatchProjectId"))
        assertTrue(source.contains("DispatchListActivity.intent"))
        assertTrue(source.contains("dispatchRepository.recent"))
        assertTrue(source.contains("projectId = projectId"))
        assertTrue(source.contains("pageSize = 5"))
    }

    private fun mainFile(relativePath: String): File = sequenceOf(
        File("src/main/$relativePath"),
        File("app/src/main/$relativePath"),
    ).firstOrNull(File::isFile) ?: error("Missing main file: $relativePath")
}
