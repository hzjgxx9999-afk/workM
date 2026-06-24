package com.qkzc.workerm

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DispatchCreateUiContractTest {
    @Test
    fun createScreenContainsReferenceDesignSectionsAndActions() {
        val layout = mainFile("res/layout/activity_dispatch_create.xml").readText()
        val requiredIds = listOf(
            "project_cover", "switch_project_button", "project_name_text",
            "project_manager_text", "project_stage_text", "title_input",
            "location_input", "dispatch_type_group", "priority_group",
            "deadline_input", "team_spinner", "team_leader_text",
            "team_worker_count_text", "content_input",
            "construction_requirement_input", "safety_notice_input",
            "before_photo_required_switch", "completion_photo_required_switch",
            "completion_min_photo_input", "acceptance_standard_input",
            "process_check_switch", "process_mode_spinner",
            "process_node_opening", "process_node_middle",
            "process_node_hidden", "process_node_final",
            "process_min_photo_input", "process_require_location_switch",
            "manager_spot_check_switch", "process_description_input",
            "drawing_attachment_button", "notice_attachment_button",
            "photo_attachment_button", "save_draft_button", "submit_button",
        )

        requiredIds.forEach { id ->
            assertTrue("missing create-page view: $id", layout.contains("@+id/$id"))
        }
        assertTrue(layout.contains("MaterialButtonToggleGroup"))
        assertTrue(layout.contains("SwitchMaterial"))
        assertTrue(layout.contains("@color/dispatch_switch_thumb"))
        assertTrue(layout.contains("@color/dispatch_switch_track"))
        assertTrue(layout.contains("保存草稿"))
        assertTrue(layout.contains("确认派工"))
    }

    @Test
    fun createScreenLoadsReadOnlyContextButDoesNotSubmitBusinessRequest() {
        val source = mainFile(
            "java/com/qkzc/workerm/ui/dispatch/DispatchCreateActivity.kt",
        ).readText()
        val themes = mainFile("res/values/themes.xml").readText()
        val manifest = mainFile("AndroidManifest.xml").readText()

        assertTrue(source.contains("loadProjectCoverBytes"))
        assertTrue(source.contains("ManagerWorkerRepository"))
        assertTrue(source.contains("showProjectSelector"))
        assertTrue(source.contains("showUiOnlyMessage"))
        assertTrue(source.contains("isAppearanceLightStatusBars = false"))
        assertTrue(themes.contains("Theme.WorkerM.DispatchCreate"))
        assertTrue(themes.contains("<item name=\"android:statusBarColor\">@color/dashboard_blue</item>"))
        assertTrue(manifest.contains("android:theme=\"@style/Theme.WorkerM.DispatchCreate\""))
        assertFalse(source.contains("DispatchRepository()"))
        assertFalse(source.contains("repository.create("))
    }

    @Test
    fun createScreenIsExportedOnlyInDebugBuildForAdbQa() {
        val mainManifest = mainFile("AndroidManifest.xml").readText()
        val debugManifest = sequenceOf(
            File("src/debug/AndroidManifest.xml"),
            File("app/src/debug/AndroidManifest.xml"),
        ).firstOrNull(File::isFile)?.readText().orEmpty()

        assertTrue(mainManifest.contains(".ui.dispatch.DispatchCreateActivity"))
        assertTrue(mainManifest.contains("android:exported=\"false\""))
        assertTrue(debugManifest.contains(".ui.dispatch.DispatchCreateActivity"))
        assertTrue(debugManifest.contains("android:exported=\"true\""))
    }

    private fun mainFile(relativePath: String): File = sequenceOf(
        File("src/main/$relativePath"),
        File("app/src/main/$relativePath"),
    ).firstOrNull(File::isFile) ?: error("Missing main file: $relativePath")
}
