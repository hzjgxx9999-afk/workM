package com.qkzc.workerm

import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ManagerLeaderOptionVo
import com.qkzc.workerm.data.network.ManagerProjectTeamSaveReq
import com.qkzc.workerm.data.network.ManagerProjectTeamStatusReq
import com.qkzc.workerm.data.network.ManagerProjectTeamVo
import com.qkzc.workerm.data.network.ManagerWorkTypeOptionVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.project.ProjectTeamRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectTeamRepositoryTest {

    @Test
    fun loadTeamsMapsTeamFields() = runBlocking {
        val api = FakeProjectTeamApi()
        val repository = ProjectTeamRepository(api)

        val teams = repository.loadTeams("token", projectId = 10)

        assertEquals("Bearer token", api.lastToken)
        assertEquals(10L, api.lastProjectId)
        assertEquals(1, teams.size)
        assertEquals(300L, teams.first().teamId)
        assertEquals(301L, teams.first().relationId)
        assertEquals("钢筋一班", teams.first().teamName)
        assertEquals(20L, teams.first().leaderId)
        assertEquals("史鸣军", teams.first().leaderName)
        assertEquals(8L, teams.first().workTypeId)
        assertEquals("钢筋工", teams.first().workTypeName)
        assertEquals("ENABLED", teams.first().status)
    }

    @Test
    fun saveAndStatusCallsUseProjectContext() = runBlocking {
        val api = FakeProjectTeamApi()
        val repository = ProjectTeamRepository(api)

        repository.createTeam(
            token = "token",
            projectId = 10,
            teamName = "木工一班",
            leaderId = 22,
            workTypeId = 9,
            remark = "东区",
        )
        repository.updateTeamStatus("token", projectId = 10, teamId = 400, status = "DISABLED")

        assertEquals(10L, api.lastProjectId)
        assertEquals("木工一班", api.lastSave?.teamName)
        assertEquals(22L, api.lastSave?.leaderId)
        assertEquals(9L, api.lastSave?.workTypeId)
        assertEquals("东区", api.lastSave?.remark)
        assertEquals(400L, api.lastTeamId)
        assertEquals("DISABLED", api.lastStatus?.status)
    }

    @Test
    fun loadLeaderOptionsMapsUserOptions() = runBlocking {
        val api = FakeProjectTeamApi()
        val repository = ProjectTeamRepository(api)

        val leaders = repository.loadLeaderOptions("token", projectId = 10, keyword = "史")

        assertEquals("史", api.lastKeyword)
        assertEquals(20L, leaders.first().leaderId)
        assertEquals("史鸣军", leaders.first().leaderName)
        assertEquals("13800000000", leaders.first().mobile)
    }

    @Test
    fun loadWorkTypeOptionsMapsEnabledWorkTypes() = runBlocking {
        val api = FakeProjectTeamApi()
        val repository = ProjectTeamRepository(api)

        val workTypes = repository.loadWorkTypeOptions("token", projectId = 10, keyword = "钢")

        assertEquals("钢", api.lastKeyword)
        assertEquals(8L, workTypes.first().workTypeId)
        assertEquals("钢筋工", workTypes.first().workTypeName)
    }

    private class FakeProjectTeamApi : SupervisorApi by EmptySupervisorApi() {
        var lastToken: String? = null
        var lastProjectId: Long? = null
        var lastTeamId: Long? = null
        var lastKeyword: String? = null
        var lastSave: ManagerProjectTeamSaveReq? = null
        var lastStatus: ManagerProjectTeamStatusReq? = null

        override suspend fun manageProjectTeams(
            token: String,
            projectId: Long,
        ): AjaxResp<List<ManagerProjectTeamVo>> {
            lastToken = token
            lastProjectId = projectId
            return AjaxResp(
                code = 200,
                msg = "ok",
                data = listOf(
                    ManagerProjectTeamVo(
                        teamId = 300,
                        relationId = 301,
                        teamName = "钢筋一班",
                        leaderId = 20,
                        leaderName = "史鸣军",
                        workTypeId = 8,
                        workTypeName = "钢筋工",
                        status = "ENABLED",
                    ),
                ),
            )
        }

        override suspend fun manageProjectLeaderOptions(
            token: String,
            projectId: Long,
            keyword: String?,
        ): AjaxResp<List<ManagerLeaderOptionVo>> {
            lastToken = token
            lastProjectId = projectId
            lastKeyword = keyword
            return AjaxResp(
                code = 200,
                msg = "ok",
                data = listOf(
                    ManagerLeaderOptionVo(
                        leaderId = 20,
                        leaderName = "史鸣军",
                        mobile = "13800000000",
                    ),
                ),
            )
        }

        override suspend fun manageProjectWorkTypes(
            token: String,
            projectId: Long,
            keyword: String?,
        ): AjaxResp<List<ManagerWorkTypeOptionVo>> {
            lastToken = token
            lastProjectId = projectId
            lastKeyword = keyword
            return AjaxResp(
                code = 200,
                msg = "ok",
                data = listOf(
                    ManagerWorkTypeOptionVo(
                        workTypeId = 8,
                        workTypeName = "钢筋工",
                    ),
                ),
            )
        }

        override suspend fun createManageProjectTeam(
            token: String,
            projectId: Long,
            body: ManagerProjectTeamSaveReq,
        ): AjaxResp<ManagerProjectTeamVo> {
            lastToken = token
            lastProjectId = projectId
            lastSave = body
            return AjaxResp(code = 200, msg = "ok", data = ManagerProjectTeamVo(teamId = 400))
        }

        override suspend fun updateManageProjectTeamStatus(
            token: String,
            projectId: Long,
            teamId: Long,
            body: ManagerProjectTeamStatusReq,
        ): AjaxResp<Any> {
            lastToken = token
            lastProjectId = projectId
            lastTeamId = teamId
            lastStatus = body
            return AjaxResp(code = 200, msg = "ok", data = null)
        }
    }
}
