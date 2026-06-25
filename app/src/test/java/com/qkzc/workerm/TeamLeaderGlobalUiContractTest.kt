package com.qkzc.workerm

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamLeaderGlobalUiContractTest {

    @Test
    fun teamLeaderScreenUsesRealAggregatedDataAndActions() {
        val layout = mainFile("res/layout/activity_team_leader.xml").readText()
        val source = mainFile("java/com/qkzc/workerm/ui/worker/TeamLeaderActivity.kt").readText()

        assertTrue(layout.contains("@+id/leader_recycler"))
        assertTrue(layout.contains("@+id/leader_empty_text"))
        assertTrue(source.contains("toTeamLeaderSummaries"))
        assertTrue(source.contains("ProjectMemberManageActivity.EXTRA_LEADER_ID"))
        assertTrue(source.contains("DispatchCreateActivity.EXTRA_TEAM_ID"))
    }

    private fun mainFile(relativePath: String): File = sequenceOf(
        File("src/main/$relativePath"),
        File("app/src/main/$relativePath"),
    ).firstOrNull(File::isFile) ?: error("Missing main file: $relativePath")
}
