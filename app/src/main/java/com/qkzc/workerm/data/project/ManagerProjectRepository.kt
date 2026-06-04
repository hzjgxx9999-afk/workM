package com.qkzc.workerm.data.project

import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ApiClient
import com.qkzc.workerm.data.network.ManagerProjectFileVo
import com.qkzc.workerm.data.network.ManagerProjectVo
import com.qkzc.workerm.data.network.ManagerTeamLeaderVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.network.bearerToken
import com.qkzc.workerm.data.network.requireSuccess

class ManagerProjectRepository(
    private val api: SupervisorApi = ApiClient.supervisorApi,
) {
    suspend fun loadProjects(token: String): List<ManagerProject> {
        return api.manageProjects(bearerToken(token)).requireDataList().map { it.toDomain() }
    }

    suspend fun loadProjectDetail(token: String, projectId: Long): ManagerProject {
        val response = api.manageProjectDetail(bearerToken(token), projectId)
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: ManagerProject(projectId = projectId)
    }

    suspend fun loadTeamLeaders(token: String, projectId: Long): List<ManagerTeamLeader> {
        return api.manageProjectTeamLeaders(bearerToken(token), projectId)
            .requireDataList()
            .map { it.toDomain() }
    }

    private fun ManagerProjectVo.toDomain(): ManagerProject {
        return ManagerProject(
            projectId = projectId ?: 0L,
            projectCode = projectCode.orEmpty(),
            projectName = projectName.orEmpty(),
            projectAddress = projectAddress.orEmpty(),
            contractAmount = contractAmount,
            employerUnit = employerUnit.orEmpty(),
            subcontractorUnit = subcontractorUnit.orEmpty(),
            financeManagerName = financeManagerName.orEmpty(),
            safetyManagerName = safetyManagerName.orEmpty(),
            projectManagerName = projectManagerName.orEmpty(),
            projectStatus = projectStatus.orEmpty(),
            progressPercent = (progressPercent ?: 0).coerceIn(0, 100),
            startDate = startDate.orEmpty(),
            plannedFinishDate = plannedFinishDate.orEmpty(),
            coverImageUrl = coverImageUrl.orEmpty(),
            daysToFinish = daysToFinish,
            workerCount = workerCount ?: 0,
            unhandledRiskCount = unhandledRiskCount ?: 0L,
            highRiskCount = highRiskCount ?: 0L,
            teamLeaders = teamLeaders.orEmpty().map { it.toDomain() },
            recentFiles = recentFiles.orEmpty().map { it.toDomain() },
        )
    }

    private fun ManagerTeamLeaderVo.toDomain(): ManagerTeamLeader {
        return ManagerTeamLeader(
            relationId = relationId ?: 0L,
            projectId = projectId ?: 0L,
            projectName = projectName.orEmpty(),
            leaderId = leaderId ?: 0L,
            leaderName = leaderName.orEmpty(),
        )
    }

    private fun <T> AjaxResp<List<T>>.requireDataList(): List<T> {
        requireSuccess(code, msg)
        return data.orEmpty()
    }

    private fun ManagerProjectFileVo.toDomain(): ManagerProjectFile {
        return ManagerProjectFile(
            fileId = fileId ?: 0L,
            fileName = fileName.orEmpty(),
            fileType = fileType.orEmpty(),
            fileSize = fileSize ?: 0L,
            category = category.orEmpty(),
            createTime = createTime.orEmpty(),
        )
    }
}

data class ManagerProject(
    val projectId: Long,
    val projectCode: String = "",
    val projectName: String = "",
    val projectAddress: String = "",
    val contractAmount: Double? = null,
    val employerUnit: String = "",
    val subcontractorUnit: String = "",
    val financeManagerName: String = "",
    val safetyManagerName: String = "",
    val projectManagerName: String = "",
    val projectStatus: String = "",
    val progressPercent: Int = 0,
    val startDate: String = "",
    val plannedFinishDate: String = "",
    val coverImageUrl: String = "",
    val daysToFinish: Long? = null,
    val workerCount: Int = 0,
    val unhandledRiskCount: Long = 0L,
    val highRiskCount: Long = 0L,
    val teamLeaders: List<ManagerTeamLeader> = emptyList(),
    val recentFiles: List<ManagerProjectFile> = emptyList(),
)

data class ManagerTeamLeader(
    val relationId: Long,
    val projectId: Long,
    val projectName: String,
    val leaderId: Long,
    val leaderName: String,
)

data class ManagerProjectFile(
    val fileId: Long,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val category: String,
    val createTime: String,
)
