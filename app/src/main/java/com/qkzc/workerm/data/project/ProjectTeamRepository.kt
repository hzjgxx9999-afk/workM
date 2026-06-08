package com.qkzc.workerm.data.project

import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ApiClient
import com.qkzc.workerm.data.network.ManagerLeaderOptionVo
import com.qkzc.workerm.data.network.ManagerProjectTeamSaveReq
import com.qkzc.workerm.data.network.ManagerProjectTeamStatusReq
import com.qkzc.workerm.data.network.ManagerProjectTeamVo
import com.qkzc.workerm.data.network.ManagerWorkTypeOptionVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.network.bearerToken
import com.qkzc.workerm.data.network.requireSuccess

class ProjectTeamRepository(
    private val api: SupervisorApi = ApiClient.supervisorApi,
) {
    suspend fun loadTeams(token: String, projectId: Long): List<ManagerProjectTeam> {
        return api.manageProjectTeams(bearerToken(token), projectId)
            .requireDataList()
            .map { it.toDomain() }
    }

    suspend fun loadLeaderOptions(
        token: String,
        projectId: Long,
        keyword: String? = null,
    ): List<ManagerLeaderOption> {
        return api.manageProjectLeaderOptions(
            token = bearerToken(token),
            projectId = projectId,
            keyword = keyword?.takeIf { it.isNotBlank() },
        ).requireDataList().map { it.toDomain() }
    }

    suspend fun loadWorkTypeOptions(
        token: String,
        projectId: Long,
        keyword: String? = null,
    ): List<ManagerWorkTypeOption> {
        return api.manageProjectWorkTypes(
            token = bearerToken(token),
            projectId = projectId,
            keyword = keyword?.takeIf { it.isNotBlank() },
        ).requireDataList().map { it.toDomain() }
    }

    suspend fun createTeam(
        token: String,
        projectId: Long,
        teamName: String,
        leaderId: Long,
        workTypeId: Long? = null,
        remark: String? = null,
    ): ManagerProjectTeam {
        val response = api.createManageProjectTeam(
            token = bearerToken(token),
            projectId = projectId,
            body = ManagerProjectTeamSaveReq(
                teamName = teamName.trim(),
                leaderId = leaderId,
                workTypeId = workTypeId,
                remark = remark?.trim()?.takeIf { it.isNotBlank() },
            ),
        )
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: ManagerProjectTeam(projectId = projectId)
    }

    suspend fun updateTeam(
        token: String,
        projectId: Long,
        teamId: Long,
        teamName: String,
        leaderId: Long,
        workTypeId: Long? = null,
        remark: String? = null,
    ): ManagerProjectTeam {
        val response = api.updateManageProjectTeam(
            token = bearerToken(token),
            projectId = projectId,
            teamId = teamId,
            body = ManagerProjectTeamSaveReq(
                teamName = teamName.trim(),
                leaderId = leaderId,
                workTypeId = workTypeId,
                remark = remark?.trim()?.takeIf { it.isNotBlank() },
            ),
        )
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: ManagerProjectTeam(projectId = projectId, teamId = teamId)
    }

    suspend fun updateTeamStatus(token: String, projectId: Long, teamId: Long, status: String) {
        val response = api.updateManageProjectTeamStatus(
            token = bearerToken(token),
            projectId = projectId,
            teamId = teamId,
            body = ManagerProjectTeamStatusReq(status),
        )
        requireSuccess(response.code, response.msg)
    }

    private fun <T> AjaxResp<List<T>>.requireDataList(): List<T> {
        requireSuccess(code, msg)
        return data.orEmpty()
    }

    private fun ManagerProjectTeamVo.toDomain(): ManagerProjectTeam {
        return ManagerProjectTeam(
            teamId = teamId ?: 0L,
            relationId = relationId ?: 0L,
            teamName = teamName.orEmpty(),
            leaderId = leaderId ?: 0L,
            leaderName = leaderName.orEmpty(),
            workTypeId = workTypeId,
            workTypeName = workTypeName.orEmpty(),
            status = status.orEmpty(),
            remark = remark.orEmpty(),
        )
    }

    private fun ManagerLeaderOptionVo.toDomain(): ManagerLeaderOption {
        return ManagerLeaderOption(
            leaderId = leaderId ?: 0L,
            leaderName = leaderName.orEmpty(),
            mobile = mobile.orEmpty(),
        )
    }

    private fun ManagerWorkTypeOptionVo.toDomain(): ManagerWorkTypeOption {
        return ManagerWorkTypeOption(
            workTypeId = workTypeId ?: 0L,
            workTypeName = workTypeName.orEmpty(),
        )
    }
}

data class ManagerProjectTeam(
    val projectId: Long = 0L,
    val teamId: Long = 0L,
    val relationId: Long = 0L,
    val teamName: String = "",
    val leaderId: Long = 0L,
    val leaderName: String = "",
    val workTypeId: Long? = null,
    val workTypeName: String = "",
    val status: String = "",
    val remark: String = "",
) {
    val enabled: Boolean
        get() = status.equals("ENABLED", ignoreCase = true)

    val statusText: String
        get() = if (enabled) "启用" else "停用"
}

data class ManagerLeaderOption(
    val leaderId: Long,
    val leaderName: String,
    val mobile: String,
)

data class ManagerWorkTypeOption(
    val workTypeId: Long,
    val workTypeName: String,
)
