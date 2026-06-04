package com.qkzc.workerm

import com.qkzc.workerm.data.network.AjaxProfileResp
import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.AjaxTokenResp
import com.qkzc.workerm.data.network.LoginReq
import com.qkzc.workerm.data.network.ManagerProjectVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.session.AuthRepository
import com.qkzc.workerm.data.session.LoginPayload
import com.qkzc.workerm.data.session.LoginSession
import com.qkzc.workerm.data.session.SessionDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthRepositoryProjectScopeTest {

    @Test
    fun loginStoresFirstManageableProjectInSession() = runBlocking {
        val sessionStore = FakeSessionDataSource()
        val api = FakeAuthProjectApi()
        val repository = AuthRepository(sessionStore, api)

        val session = repository.login(LoginPayload(mobile = "13900010001", password = "123456"))

        assertEquals("Bearer login-token", api.projectsToken)
        assertEquals("10", session.projectId)
        assertEquals("一号项目", session.projectName)
        assertEquals(session, sessionStore.savedSession)
    }

    private class FakeAuthProjectApi : SupervisorApi by EmptySupervisorApi() {
        var projectsToken: String? = null

        override suspend fun login(body: LoginReq): AjaxTokenResp {
            return AjaxTokenResp(code = 200, msg = "ok", token = "login-token")
        }

        override suspend fun profile(token: String): AjaxProfileResp {
            return AjaxProfileResp(
                code = 200,
                msg = "ok",
                appUserId = 300,
                mobile = "13900010001",
                userType = "PROJECT_MANAGER",
                clientType = "APP",
            )
        }

        override suspend fun manageProjects(token: String): AjaxResp<List<ManagerProjectVo>> {
            projectsToken = token
            return AjaxResp(
                code = 200,
                msg = "ok",
                data = listOf(
                    ManagerProjectVo(projectId = 10, projectName = "一号项目"),
                ),
            )
        }
    }

    private class FakeSessionDataSource : SessionDataSource {
        private val state = MutableStateFlow(LoginSession())
        var savedSession: LoginSession? = null

        override val sessionFlow: Flow<LoginSession> = state

        override suspend fun saveSession(session: LoginSession) {
            savedSession = session
            state.value = session
        }

        override suspend fun clearSession() {
            state.value = LoginSession()
        }
    }
}
