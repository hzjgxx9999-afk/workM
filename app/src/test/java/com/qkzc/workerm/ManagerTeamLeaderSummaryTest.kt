package com.qkzc.workerm

import com.qkzc.workerm.data.project.ManagerProject
import com.qkzc.workerm.data.project.ManagerProjectTeam
import com.qkzc.workerm.data.worker.ManagerWorker
import com.qkzc.workerm.data.worker.toTeamLeaderSummaries
import org.junit.Assert.assertEquals
import org.junit.Test

class ManagerTeamLeaderSummaryTest {

    @Test
    fun aggregatesLeaderTeamsAcrossAllManagedProjects() {
        val projects = listOf(
            ManagerProject(projectId = 100, projectName = "未来城二期"),
            ManagerProject(projectId = 200, projectName = "科创园"),
        )
        val teamsByProject = mapOf(
            100L to listOf(
                team(projectId = 100, teamId = 1, leaderId = 10, leaderName = "李师傅", teamName = "钢筋班组"),
                team(projectId = 100, teamId = 2, leaderId = 11, leaderName = "王师傅", teamName = "模板班组"),
            ),
            200L to listOf(
                team(projectId = 200, teamId = 3, leaderId = 10, leaderName = "李师傅", teamName = "架子班组"),
            ),
        )
        val workersByProject = mapOf(
            100L to listOf(
                worker(projectId = 100, workerUserId = 10, leaderId = 10, teamId = 1, bindStatus = "ACTIVE", mobile = "13800001111"),
                worker(projectId = 100, workerUserId = 12, leaderId = 10, teamId = 1, bindStatus = "ACTIVE"),
                worker(projectId = 100, workerUserId = 13, leaderId = 10, teamId = 1, bindStatus = "ABNORMAL"),
                worker(projectId = 100, workerUserId = 14, leaderId = 11, teamId = 2, bindStatus = "ACTIVE"),
            ),
            200L to listOf(
                worker(projectId = 200, workerUserId = 15, leaderId = 10, teamId = 3, bindStatus = "ENTERING"),
            ),
        )

        val summaries = toTeamLeaderSummaries(projects, teamsByProject, workersByProject)

        assertEquals(listOf(10L, 11L), summaries.map { it.leaderId })
        assertEquals("李师傅", summaries.first().leaderName)
        assertEquals("13800001111", summaries.first().mobile)
        assertEquals(2, summaries.first().projectCount)
        assertEquals(2, summaries.first().teamCount)
        assertEquals(4, summaries.first().workerCount)
        assertEquals(1, summaries.first().abnormalCount)
        assertEquals(listOf("未来城二期", "科创园"), summaries.first().scopes.map { it.projectName })
    }

    private fun team(
        projectId: Long,
        teamId: Long,
        leaderId: Long,
        leaderName: String,
        teamName: String,
    ) = ManagerProjectTeam(
        projectId = projectId,
        teamId = teamId,
        leaderId = leaderId,
        leaderName = leaderName,
        teamName = teamName,
        status = "ENABLED",
    )

    private fun worker(
        projectId: Long,
        workerUserId: Long,
        leaderId: Long,
        teamId: Long,
        bindStatus: String,
        mobile: String = "",
    ) = ManagerWorker(
        projectId = projectId,
        workerUserId = workerUserId,
        leaderId = leaderId,
        teamId = teamId,
        bindStatus = bindStatus,
        mobile = mobile,
    )
}
