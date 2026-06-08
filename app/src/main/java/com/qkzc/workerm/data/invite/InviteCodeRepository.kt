package com.qkzc.workerm.data.invite

import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ApiClient
import com.qkzc.workerm.data.network.ManageInviteCodeCreateReq
import com.qkzc.workerm.data.network.ManageInviteCodeStatusReq
import com.qkzc.workerm.data.network.ManageInviteCodeVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.network.bearerToken
import com.qkzc.workerm.data.network.requireSuccess

class InviteCodeRepository(
    private val api: SupervisorApi = ApiClient.supervisorApi,
) {
    suspend fun loadList(
        token: String,
        projectId: Long? = null,
        status: String? = null,
        inviteCode: String? = null,
    ): List<ManageInviteCode> {
        return api.manageInviteCodeList(
            token = bearerToken(token),
            projectId = projectId,
            status = status?.takeIf { it.isNotBlank() },
            inviteCode = inviteCode?.takeIf { it.isNotBlank() },
        ).requireDataList().map { it.toDomain() }
    }

    suspend fun create(
        token: String,
        projectId: Long,
        leaderId: Long,
        teamId: Long,
        maxUseCount: Int? = null,
        expireTime: String? = null,
        remark: String? = null,
    ): ManageInviteCode {
        val response = api.createManageInviteCode(
            bearerToken(token),
            ManageInviteCodeCreateReq(
                projectId = projectId,
                leaderId = leaderId,
                teamId = teamId,
                expireTime = expireTime?.takeIf { it.isNotBlank() },
                maxUseCount = maxUseCount,
                remark = remark?.takeIf { it.isNotBlank() },
            ),
        )
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: error("生成成功但未返回邀请码")
    }

    suspend fun updateStatus(token: String, id: Long, status: String) {
        val response = api.updateManageInviteCodeStatus(
            bearerToken(token),
            id,
            ManageInviteCodeStatusReq(status),
        )
        requireSuccess(response.code, response.msg)
    }

    private fun <T> AjaxResp<List<T>>.requireDataList(): List<T> {
        requireSuccess(code, msg)
        return data.orEmpty()
    }

    private fun ManageInviteCodeVo.toDomain(): ManageInviteCode {
        val content = qrContent?.takeIf { it.isNotBlank() }
            ?: registerDeepLink?.takeIf { it.isNotBlank() }
            ?: inviteCode.orEmpty()
        return ManageInviteCode(
            id = id ?: 0L,
            inviteCode = inviteCode.orEmpty(),
            projectId = projectId ?: 0L,
            projectName = projectName.orEmpty(),
            leaderId = leaderId ?: 0L,
            leaderName = leaderName.orEmpty(),
            teamId = teamId,
            teamName = teamName.orEmpty(),
            expireTime = expireTime.orEmpty(),
            maxUseCount = maxUseCount ?: 0,
            usedCount = usedCount ?: 0,
            status = status.orEmpty(),
            remark = remark.orEmpty(),
            registerDeepLink = registerDeepLink.orEmpty(),
            qrContent = content,
        )
    }
}

data class ManageInviteCode(
    val id: Long,
    val inviteCode: String,
    val projectId: Long,
    val projectName: String,
    val leaderId: Long,
    val leaderName: String,
    val teamId: Long?,
    val teamName: String,
    val expireTime: String,
    val maxUseCount: Int,
    val usedCount: Int,
    val status: String,
    val remark: String,
    val registerDeepLink: String,
    val qrContent: String,
) {
    val enabled: Boolean
        get() = status.equals("ENABLED", ignoreCase = true)

    val statusText: String
        get() = if (enabled) "启用" else "停用"

    val usageText: String
        get() = if (maxUseCount > 0) "$usedCount/$maxUseCount" else "$usedCount/不限"
}
