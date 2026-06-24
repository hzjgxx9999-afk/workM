package com.qkzc.workerm

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DispatchAcceptanceUiContractTest {
    @Test
    fun acceptanceDetailUsesStructuredReferenceDesignAndFixedActions() {
        val layout = mainFile("res/layout/activity_dispatch_detail.xml").readText()
        val ids = listOf(
            "status_text", "title_text", "phase_text", "menu_button",
            "dispatch_no_text", "project_text", "team_text", "leader_text",
            "location_text", "deadline_text", "summary_section", "summary_time_text",
            "summary_content_text", "worker_list", "process_list", "evidence_list",
            "spot_check_banner", "spot_check_action", "acceptance_section",
            "acceptance_input", "action_bar", "reject_button", "approve_button",
        )
        ids.forEach { assertTrue("missing acceptance view: $it", layout.contains("@+id/$it")) }
        assertTrue(layout.contains("MaterialCardView"))
        assertTrue(layout.contains("RecyclerView"))
        assertTrue(layout.contains("TextInputEditText"))
        assertTrue(layout.indexOf("@+id/action_bar") > layout.indexOf("</androidx.core.widget.NestedScrollView>"))
    }

    @Test
    fun workerAcceptanceRowDoesNotRenderAvatarPlaceholder() {
        val layout = mainFile("res/layout/item_dispatch_acceptance_worker.xml").readText()

        assertFalse(layout.contains("@drawable/bg_icon_blue"))
    }

    @Test
    fun spotCheckReviewIsAFullScreenPhotoCentricWorkflow() {
        val layout = mainFile("res/layout/activity_spot_check_review.xml").readText()
        val source = mainFile("java/com/qkzc/workerm/ui/dispatch/SpotCheckReviewActivity.kt").readText()
        val manifest = mainFile("AndroidManifest.xml").readText()
        val detailSource = mainFile("java/com/qkzc/workerm/ui/dispatch/DispatchDetailActivity.kt").readText()

        listOf(
            "operation_record_button", "spot_status_text", "order_info_card",
            "spot_description_card", "photo_list", "location_text",
            "linked_process_list", "review_input", "reject_button", "pass_button",
        ).forEach { assertTrue("missing spot-check view: $it", layout.contains("@+id/$it")) }
        assertTrue(source.contains("reviewSpotCheck"))
        assertTrue(source.contains("MAX_REVIEW_LENGTH = 200"))
        assertTrue(manifest.contains(".ui.dispatch.SpotCheckReviewActivity"))
        assertTrue(detailSource.contains("SpotCheckReviewActivity.intent"))
        assertFalse(detailSource.contains("showManagerProcessReviewDialog"))
    }

    @Test
    fun managerUsesAggregateContractsAndCoil() {
        val api = mainFile("java/com/qkzc/workerm/data/dispatch/DispatchApi.kt").readText()
        val models = mainFile("java/com/qkzc/workerm/data/dispatch/DispatchModels.kt").readText()
        val catalog = rootFile("gradle/libs.versions.toml").readText()
        val build = rootFile("app/build.gradle.kts").readText()

        assertTrue(api.contains("/{id}/acceptance-detail"))
        assertTrue(api.contains("/{id}/spot-checks/{nodeId}"))
        assertTrue(api.contains("/{id}/spot-checks"))
        assertTrue(models.contains("data class DispatchAcceptanceDetail"))
        assertTrue(models.contains("data class DispatchSpotCheckDetail"))
        assertTrue(catalog.contains("coil"))
        assertTrue(build.contains("implementation(libs.coil)"))
    }

    private fun mainFile(relativePath: String): File = sequenceOf(
        File("src/main/$relativePath"), File("app/src/main/$relativePath"),
    ).firstOrNull(File::isFile) ?: error("Missing main file: $relativePath")

    private fun rootFile(relativePath: String): File = sequenceOf(
        File(relativePath), File("../$relativePath"),
    ).firstOrNull(File::isFile) ?: error("Missing root file: $relativePath")
}
