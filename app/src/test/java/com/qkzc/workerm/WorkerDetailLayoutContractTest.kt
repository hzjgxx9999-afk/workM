package com.qkzc.workerm

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class WorkerDetailLayoutContractTest {

    @Test
    fun workerDetailLayoutExposesBindableWorkerFields() {
        val xml = Files.readString(Path.of("src/main/res/layout/activity_worker_detail.xml"))

        listOf(
            "worker_name_text",
            "worker_type_text",
            "worker_status_text",
            "worker_team_text",
            "worker_leader_text",
            "worker_project_text",
            "worker_mobile_text",
            "worker_id_card_text",
            "entry_progress_title_text",
            "identity_status_text",
            "safety_status_text",
            "health_status_text",
            "contract_status_text",
            "insurance_status_text",
        ).forEach { id ->
            assertTrue("missing @$id", xml.contains("@+id/$id") || xml.contains("@id/$id"))
        }
    }
}
