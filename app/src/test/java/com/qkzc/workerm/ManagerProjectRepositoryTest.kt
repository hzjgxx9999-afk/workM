package com.qkzc.workerm

import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ManagerProjectVo
import com.qkzc.workerm.data.network.ManagerTeamLeaderVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.project.ManagerProjectRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ManagerProjectRepositoryTest {

    @Test
    fun loadProjectsMapsBackendFieldsToDomain() = runBlocking {
        val api = FakeProjectApi(
            projects = listOf(
                ManagerProjectVo(
                    projectId = 10,
                    projectCode = "P001",
                    projectName = "一号项目",
                    projectAddress = "广州",
                    projectManagerName = "王经理",
                ),
            ),
        )
        val repository = ManagerProjectRepository(api)

        val projects = repository.loadProjects("token")

        assertEquals("Bearer token", api.lastToken)
        assertEquals(1, projects.size)
        assertEquals(10, projects.first().projectId)
        assertEquals("一号项目", projects.first().projectName)
        assertEquals("广州", projects.first().projectAddress)
    }

    private class FakeProjectApi(
        private val projects: List<ManagerProjectVo>,
    ) : SupervisorApi by EmptySupervisorApi() {
        var lastToken: String? = null

        override suspend fun manageProjects(token: String): AjaxResp<List<ManagerProjectVo>> {
            lastToken = token
            return AjaxResp(code = 200, msg = "ok", data = projects)
        }

        override suspend fun manageProjectDetail(
            token: String,
            projectId: Long,
        ): AjaxResp<ManagerProjectVo> {
            return AjaxResp(code = 200, msg = "ok", data = projects.first { it.projectId == projectId })
        }

        override suspend fun manageProjectTeamLeaders(
            token: String,
            projectId: Long,
        ): AjaxResp<List<ManagerTeamLeaderVo>> {
            return AjaxResp(code = 200, msg = "ok", data = emptyList())
        }
    }
}
