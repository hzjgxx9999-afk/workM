package com.qkzc.workerm

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectMemberGlobalUiContractTest {

    @Test
    fun memberPageContainsGlobalWorkerTabsAndProjectScopedEntryStillExists() {
        val layout = mainFile("res/layout/fragment_project_member_manage.xml").readText()
        val mainActivity = mainFile("java/com/qkzc/workerm/MainActivity.kt").readText()
        val projectDetail = mainFile("java/com/qkzc/workerm/ui/project/ProjectDetailActivity.kt").readText()

        assertTrue(layout.contains("@+id/tab_leader"))
        assertTrue(layout.contains("@+id/tab_worker"))
        assertTrue(mainActivity.contains("ProjectMemberManageFragment.newGlobalInstance()"))
        assertTrue(projectDetail.contains("openProjectScoped(ProjectMemberManageActivity::class.java)"))
    }

    @Test
    fun dispatchCreateSupportsProjectTeamLeaderPreselection() {
        val source = mainFile("java/com/qkzc/workerm/ui/dispatch/DispatchCreateActivity.kt").readText()

        assertTrue(source.contains("EXTRA_PROJECT_ID"))
        assertTrue(source.contains("EXTRA_TEAM_ID"))
        assertTrue(source.contains("EXTRA_LEADER_ID"))
        assertTrue(source.contains("preselectedProjectId"))
        assertTrue(source.contains("preselectedTeamId"))
        assertTrue(source.contains("preselectedLeaderId"))
    }

    private fun mainFile(relativePath: String): File = sequenceOf(
        File("src/main/$relativePath"),
        File("app/src/main/$relativePath"),
    ).firstOrNull(File::isFile) ?: error("Missing main file: $relativePath")
}
