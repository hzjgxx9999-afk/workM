package com.qkzc.workerm

import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ManageInviteCodeCreateReq
import com.qkzc.workerm.data.network.ManageInviteCodeStatusReq
import com.qkzc.workerm.data.network.ManageInviteCodeVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.invite.InviteCodeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.io.File

class InviteCodeManageContractTest {

    @Test
    fun supervisorApiDefinesManagerInviteCodeRoutes() {
        assertEquals("/app/manage/invite-code/list", apiPath("manageInviteCodeList", GET::class.java))
        assertEquals("/app/manage/invite-code", apiPath("createManageInviteCode", POST::class.java))
        assertEquals("/app/manage/invite-code/{id}/status", apiPath("updateManageInviteCodeStatus", PUT::class.java))

        val create = method("createManageInviteCode")
        assertNotNull(create.parameters.firstOrNull { it.isAnnotationPresent(Body::class.java) })

        val status = method("updateManageInviteCodeStatus")
        assertNotNull(status.parameters.firstOrNull { it.isAnnotationPresent(Path::class.java) })
        assertNotNull(status.parameters.firstOrNull { it.isAnnotationPresent(Body::class.java) })
    }

    @Test
    fun supervisorApiDefinesProjectTeamRoutes() {
        assertEquals("/app/manage/projects/{projectId}/teams", apiPath("manageProjectTeams", GET::class.java))
        assertEquals("/app/manage/projects/{projectId}/leader-options", apiPath("manageProjectLeaderOptions", GET::class.java))
        assertEquals("/app/manage/projects/{projectId}/work-types", apiPath("manageProjectWorkTypes", GET::class.java))
        assertEquals("/app/manage/projects/{projectId}/teams", apiPath("createManageProjectTeam", POST::class.java))
        assertEquals("/app/manage/projects/{projectId}/teams/{teamId}", apiPath("updateManageProjectTeam", PUT::class.java))
        assertEquals("/app/manage/projects/{projectId}/teams/{teamId}/status", apiPath("updateManageProjectTeamStatus", PUT::class.java))

        val create = method("createManageProjectTeam")
        assertNotNull(create.parameters.firstOrNull { it.isAnnotationPresent(Path::class.java) })
        assertNotNull(create.parameters.firstOrNull { it.isAnnotationPresent(Body::class.java) })

        val status = method("updateManageProjectTeamStatus")
        assertNotNull(status.parameters.firstOrNull { it.isAnnotationPresent(Path::class.java) })
        assertNotNull(status.parameters.firstOrNull { it.isAnnotationPresent(Body::class.java) })
    }

    @Test
    fun repositoryMapsCreateAndStatusFlow() = runBlocking {
        val api = FakeInviteApi()
        val repository = InviteCodeRepository(api)

        val created = repository.create(
            token = "token",
            projectId = 10,
            leaderId = 20,
            teamId = 30,
            maxUseCount = 3,
            expireTime = "2026-06-30 00:00:00",
            remark = "现场招工",
        )
        repository.updateStatus("token", created.id, "DISABLED")

        assertEquals("Bearer token", api.lastToken)
        assertEquals(10L, api.lastCreate?.projectId)
        assertEquals(20L, api.lastCreate?.leaderId)
        assertEquals(30L, api.lastCreate?.teamId)
        assertEquals("workerapp://register?inviteCode=INV100", created.qrContent)
        assertEquals("DISABLED", api.lastStatus?.status)
    }

    @Test
    fun inviteCodeCreateRequiresConcreteTeamId() {
        val repositorySource = File("src/main/java/com/qkzc/workerm/data/invite/InviteCodeRepository.kt").readText()
        val networkSource = File("src/main/java/com/qkzc/workerm/data/network/NetworkModels.kt").readText()

        assertTrue(repositorySource.contains("teamId: Long,"))
        assertTrue(networkSource.contains("val teamId: Long,"))
    }

    @Test
    fun fragmentLayoutAndHomeEntryExist() {
        Class.forName("com.qkzc.workerm.ui.invite.InviteCodeManageFragment")

        val layout = File("src/main/res/layout/fragment_invite_code_manage.xml").readText()
        assertTrue(layout.contains("@+id/spinner_project"))
        assertTrue(layout.contains("@+id/spinner_team"))
        assertTrue(layout.contains("@+id/button_create_invite"))
        assertTrue(layout.contains("@+id/recycler_invite_codes"))

        val home = File("src/main/res/layout/fragment_home.xml").readText()
        assertTrue(home.contains("@+id/invite_code_action"))
        assertTrue(home.contains("@+id/home_project_recycler"))
        assertTrue(!home.contains("item_static_project_"))

        val homeFragment = File("src/main/java/com/qkzc/workerm/ui/home/HomeFragment.kt").readText()
        assertTrue(homeFragment.contains("openInviteCodeManage"))

        val detail = File("src/main/res/layout/activity_project_detail.xml").readText()
        assertTrue(detail.contains("@+id/project_members_action"))
        assertTrue(detail.contains("@+id/invite_code_action"))
        assertTrue(detail.contains("@+id/recent_files_container"))
        assertTrue(!detail.contains("item_detail_file_"))
    }

    private class FakeInviteApi : SupervisorApi by EmptySupervisorApi() {
        var lastToken: String? = null
        var lastCreate: ManageInviteCodeCreateReq? = null
        var lastStatus: ManageInviteCodeStatusReq? = null

        override suspend fun createManageInviteCode(
            token: String,
            body: ManageInviteCodeCreateReq,
        ): AjaxResp<ManageInviteCodeVo> {
            lastToken = token
            lastCreate = body
            return AjaxResp(
                code = 200,
                msg = "ok",
                data = ManageInviteCodeVo(
                    id = 100,
                    inviteCode = "INV100",
                    projectId = body.projectId,
                    projectName = "北京项目",
                    leaderId = body.leaderId,
                    leaderName = "史鸣军",
                    maxUseCount = body.maxUseCount,
                    expireTime = body.expireTime,
                    status = "ENABLED",
                    qrContent = "workerapp://register?inviteCode=INV100",
                ),
            )
        }

        override suspend fun updateManageInviteCodeStatus(
            token: String,
            id: Long,
            body: ManageInviteCodeStatusReq,
        ): AjaxResp<Any> {
            lastToken = token
            lastStatus = body
            return AjaxResp(code = 200, msg = "ok", data = null)
        }
    }

    private fun apiPath(methodName: String, annotationType: Class<out Annotation>): String {
        val annotation = method(methodName).getAnnotation(annotationType)
        return when (annotation) {
            is GET -> annotation.value
            is POST -> annotation.value
            is PUT -> annotation.value
            else -> error("unsupported annotation")
        }
    }

    private fun method(methodName: String) =
        SupervisorApi::class.java.methods.firstOrNull { it.name == methodName }
            ?: error("Missing SupervisorApi.$methodName")
}
