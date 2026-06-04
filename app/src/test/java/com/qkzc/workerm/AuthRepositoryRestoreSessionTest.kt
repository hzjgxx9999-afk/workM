package com.qkzc.workerm

import com.qkzc.workerm.data.network.AdvanceAuditReq
import com.qkzc.workerm.data.network.AdvanceDetailReq
import com.qkzc.workerm.data.network.AdvanceRequestVo
import com.qkzc.workerm.data.network.AiWarningDetailReq
import com.qkzc.workerm.data.network.AiWarningHandleReq
import com.qkzc.workerm.data.network.AiWarningListReq
import com.qkzc.workerm.data.network.AiWarningPageVo
import com.qkzc.workerm.data.network.AiWarningReadReq
import com.qkzc.workerm.data.network.AiWarningVo
import com.qkzc.workerm.data.network.AjaxProfileResp
import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.AjaxTokenResp
import com.qkzc.workerm.data.network.ApiException
import com.qkzc.workerm.data.network.AttendanceExceptionVo
import com.qkzc.workerm.data.network.AuditListReq
import com.qkzc.workerm.data.network.ExceptionAuditReq
import com.qkzc.workerm.data.network.ExceptionDetailReq
import com.qkzc.workerm.data.network.LoginReq
import com.qkzc.workerm.data.network.ManagerProjectVo
import com.qkzc.workerm.data.network.MaterialAuditReq
import com.qkzc.workerm.data.network.MaterialDetailReq
import com.qkzc.workerm.data.network.MaterialListReq
import com.qkzc.workerm.data.network.MaterialRequestVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.session.AuthRepository
import com.qkzc.workerm.data.session.LoginSession
import com.qkzc.workerm.data.session.SessionDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryRestoreSessionTest {

    @Test
    fun emptyTokenDoesNotCallProfileAndReturnsNull() = runBlocking {
        val sessionStore = FakeSessionDataSource(LoginSession())
        val api = FakeSupervisorApi()
        val repository = AuthRepository(sessionStore, api)

        val restored = repository.restoreValidSession()

        assertNull(restored)
        assertFalse(api.profileCalled)
        assertFalse(sessionStore.clearCalled)
    }

    @Test
    fun invalidCachedTokenIsClearedAndRoutesToLogin() = runBlocking {
        val sessionStore = FakeSessionDataSource(LoginSession(accessToken = "old-token"))
        val api = FakeSupervisorApi(profileError = ApiException("401"))
        val repository = AuthRepository(sessionStore, api)

        val restored = repository.restoreValidSession()

        assertNull(restored)
        assertEquals("Bearer old-token", api.profileToken)
        assertTrue(sessionStore.clearCalled)
        assertEquals(LoginSession(), sessionStore.currentSession)
    }

    @Test
    fun nonProjectManagerCachedTokenIsCleared() = runBlocking {
        val sessionStore = FakeSessionDataSource(LoginSession(accessToken = "worker-token"))
        val api = FakeSupervisorApi(
            profileResponse = AjaxProfileResp(
                code = 200,
                msg = "ok",
                appUserId = 101,
                mobile = "13900010002",
                userType = "WORKER",
                clientType = "APP",
            ),
        )
        val repository = AuthRepository(sessionStore, api)

        val restored = repository.restoreValidSession()

        assertNull(restored)
        assertTrue(sessionStore.clearCalled)
    }

    @Test
    fun validProjectManagerCachedTokenIsRefreshedAndKept() = runBlocking {
        val sessionStore = FakeSessionDataSource(LoginSession(accessToken = "valid-token"))
        val api = FakeSupervisorApi(
            profileResponse = AjaxProfileResp(
                code = 200,
                msg = "ok",
                appUserId = 201,
                mobile = "13900010001",
                userType = "PROJECT_MANAGER",
                clientType = "MANAGE_APP",
            ),
        )
        val repository = AuthRepository(sessionStore, api)

        val restored = repository.restoreValidSession()

        assertEquals("valid-token", restored?.accessToken)
        assertEquals("201", restored?.userId)
        assertEquals("PROJECT_MANAGER", restored?.userType)
        assertEquals("Bearer valid-token", api.profileToken)
        assertEquals(restored, sessionStore.savedSession)
        assertFalse(sessionStore.clearCalled)
    }

    private class FakeSessionDataSource(
        initialSession: LoginSession,
    ) : SessionDataSource {
        private val state = MutableStateFlow(initialSession)
        var clearCalled = false
        var savedSession: LoginSession? = null
        val currentSession: LoginSession
            get() = state.value

        override val sessionFlow: Flow<LoginSession> = state

        override suspend fun saveSession(session: LoginSession) {
            savedSession = session
            state.value = session
        }

        override suspend fun clearSession() {
            clearCalled = true
            state.value = LoginSession()
        }
    }

    private class FakeSupervisorApi(
        private val profileResponse: AjaxProfileResp = AjaxProfileResp(
            code = 200,
            msg = "ok",
            appUserId = 201,
            mobile = "13900010001",
            userType = "PROJECT_MANAGER",
            clientType = "MANAGE_APP",
        ),
        private val profileError: RuntimeException? = null,
    ) : SupervisorApi by EmptySupervisorApi() {
        var profileCalled = false
        var profileToken: String? = null

        override suspend fun login(body: LoginReq): AjaxTokenResp = error("unused")

        override suspend fun profile(token: String): AjaxProfileResp {
            profileCalled = true
            profileToken = token
            profileError?.let { throw it }
            return profileResponse
        }

        override suspend fun manageProjects(token: String): AjaxResp<List<ManagerProjectVo>> {
            return AjaxResp(200, "ok", emptyList())
        }

        override suspend fun advanceList(
            token: String,
            body: AuditListReq,
        ): AjaxResp<List<AdvanceRequestVo>> = error("unused")

        override suspend fun advanceDetail(
            token: String,
            body: AdvanceDetailReq,
        ): AjaxResp<AdvanceRequestVo> = error("unused")

        override suspend fun auditAdvance(
            token: String,
            body: AdvanceAuditReq,
        ): AjaxResp<AdvanceRequestVo> = error("unused")

        override suspend fun materialList(
            token: String,
            body: MaterialListReq,
        ): AjaxResp<List<MaterialRequestVo>> = error("unused")

        override suspend fun materialDetail(
            token: String,
            body: MaterialDetailReq,
        ): AjaxResp<MaterialRequestVo> = error("unused")

        override suspend fun auditMaterial(
            token: String,
            body: MaterialAuditReq,
        ): AjaxResp<MaterialRequestVo> = error("unused")

        override suspend fun exceptionList(
            token: String,
            body: AuditListReq,
        ): AjaxResp<List<AttendanceExceptionVo>> = error("unused")

        override suspend fun exceptionDetail(
            token: String,
            body: ExceptionDetailReq,
        ): AjaxResp<AttendanceExceptionVo> = error("unused")

        override suspend fun auditException(
            token: String,
            body: ExceptionAuditReq,
        ): AjaxResp<AttendanceExceptionVo> = error("unused")

        override suspend fun manageAiWarningList(
            token: String,
            body: AiWarningListReq,
        ): AjaxResp<AiWarningPageVo> = error("unused")

        override suspend fun manageAiWarningDetail(
            token: String,
            body: AiWarningDetailReq,
        ): AjaxResp<AiWarningVo> = error("unused")

        override suspend fun manageAiWarningRead(
            token: String,
            body: AiWarningReadReq,
        ): AjaxResp<Any> = error("unused")

        override suspend fun manageAiWarningHandle(
            token: String,
            body: AiWarningHandleReq,
        ): AjaxResp<Any> = error("unused")

        override suspend fun manageAiWarningUnreadCount(
            token: String,
            body: Map<String, String>,
        ): AjaxResp<Int> = error("unused")
    }
}
