package com.qkzc.workerm

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DispatchProcessCreateContractTest {
    @Test
    fun managerCreateRequestCarriesProcessCheckAndAcceptanceFields() {
        val models = File("src/main/java/com/qkzc/workerm/data/dispatch/DispatchModels.kt").readText()

        assertTrue(models.contains("constructionRequirement"))
        assertTrue(models.contains("safetyNotice"))
        assertTrue(models.contains("acceptanceStandard"))
        assertTrue(models.contains("processCheckDescription"))
        assertTrue(models.contains("processCheckEnabled"))
        assertTrue(models.contains("processCheckMode"))
        assertTrue(models.contains("processNodes"))
        assertTrue(models.contains("processMinPhotoCount"))
        assertTrue(models.contains("processRequireLocation"))
        assertTrue(models.contains("managerSpotCheckEnabled"))
        assertTrue(models.contains("beforePhotoRequired"))
        assertTrue(models.contains("completionPhotoRequired"))
        assertTrue(models.contains("completionMinPhotoCount"))
    }

    @Test
    fun managerCreateScreenSubmitsProcessCheckControlsWithBusinessRequest() {
        val layout = File("src/main/res/layout/activity_dispatch_create.xml").readText()
        val activity = File("src/main/java/com/qkzc/workerm/ui/dispatch/DispatchCreateActivity.kt").readText()

        assertTrue(layout.contains("process_check_switch"))
        assertTrue(layout.contains("process_mode_spinner"))
        assertTrue(layout.contains("process_node_opening"))
        assertTrue(layout.contains("process_node_middle"))
        assertTrue(layout.contains("process_node_hidden"))
        assertTrue(layout.contains("process_min_photo_input"))
        assertTrue(layout.contains("process_require_location_switch"))
        assertTrue(layout.contains("manager_spot_check_switch"))
        assertTrue(activity.contains("setProcessSectionVisible"))
        assertTrue(activity.contains("buildProcessNodes"))
        assertTrue(activity.contains("repository.create("))
        assertTrue(!activity.contains("showUiOnlyMessage"))
    }
}
