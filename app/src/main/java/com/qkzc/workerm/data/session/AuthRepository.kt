package com.qkzc.workerm.data.session

import com.qkzc.workerm.data.network.ApiClient
import com.qkzc.workerm.data.network.ApiException
import com.qkzc.workerm.data.network.ApprovalApiConstants
import com.qkzc.workerm.data.network.LoginReq
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.network.bearerToken
import com.qkzc.workerm.data.network.requireSuccess
import com.qkzc.workerm.data.project.ManagerProjectRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val sessionStore: SessionDataSource,
    private val api: SupervisorApi = ApiClient.supervisorApi,
    private val projectRepository: ManagerProjectRepository = ManagerProjectRepository(api),
) {

    val sessionFlow = sessionStore.sessionFlow

    suspend fun login(payload: LoginPayload): LoginSession {
        val tokenResp = api.login(
            LoginReq(
                mobile = payload.mobile.trim(),
                password = payload.password,
            ),
        )
        requireSuccess(tokenResp.code, tokenResp.msg)

        val token = tokenResp.token?.takeIf { it.isNotBlank() }
            ?: throw ApiException("登录成功但后端未返回 token")
        val profile = api.profile(bearerToken(token))
        requireSuccess(profile.code, profile.msg)

        if (profile.userType != ApprovalApiConstants.USER_TYPE_PROJECT_MANAGER) {
            throw ApiException("当前账号不是项目经理，不能登录监管端")
        }
        val firstProject = projectRepository.loadProjects(token).firstOrNull()

        val session = LoginSession(
            accessToken = token,
            userId = profile.appUserId?.toString().orEmpty(),
            userName = profile.mobile.orEmpty(),
            mobile = profile.mobile.orEmpty(),
            userType = profile.userType.orEmpty(),
            clientType = profile.clientType.orEmpty(),
            roleName = "项目经理",
            organizationName = firstProject?.projectName ?: "全部项目",
            projectId = firstProject?.projectId?.toString().orEmpty(),
            projectName = firstProject?.projectName ?: "全部项目",
        )
        sessionStore.saveSession(session)
        return session
    }

    suspend fun restoreValidSession(): LoginSession? {
        val cached = sessionStore.sessionFlow.first()
        if (!cached.isLoggedIn) return null

        return try {
            val profile = api.profile(bearerToken(cached.accessToken))
            requireSuccess(profile.code, profile.msg)
            if (profile.userType != ApprovalApiConstants.USER_TYPE_PROJECT_MANAGER) {
                throw ApiException("当前账号不是项目经理，不能登录监管端")
            }
            val firstProject = projectRepository.loadProjects(cached.accessToken).firstOrNull()
            val refreshed = cached.copy(
                userId = profile.appUserId?.toString().orEmpty(),
                userName = profile.mobile.orEmpty(),
                mobile = profile.mobile.orEmpty(),
                userType = profile.userType.orEmpty(),
                clientType = profile.clientType.orEmpty(),
                roleName = "项目经理",
                organizationName = firstProject?.projectName ?: cached.organizationName.ifBlank { "全部项目" },
                projectId = firstProject?.projectId?.toString() ?: cached.projectId,
                projectName = firstProject?.projectName ?: cached.projectName.ifBlank { "全部项目" },
            )
            sessionStore.saveSession(refreshed)
            refreshed
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            sessionStore.clearSession()
            null
        }
    }

    suspend fun logout() {
        sessionStore.clearSession()
    }
}
