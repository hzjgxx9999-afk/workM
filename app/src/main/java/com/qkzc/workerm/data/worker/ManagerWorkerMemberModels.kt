package com.qkzc.workerm.data.worker

import com.qkzc.workerm.data.project.ManagerProject
import com.qkzc.workerm.data.project.ManagerProjectTeam
import java.util.Locale

enum class ManagerWorkerFilter(
    val title: String,
    val backendStatus: String?,
) {
    ALL("全部", null),
    TEAM_LEADER("班组长", null),
    WORKER("工人", null),
    ENTERING("待入场", "ENTERING"),
    EXITED("已离场", "EXITED"),
}

enum class ManagerWorkerPageMode {
    GLOBAL,
    PROJECT,
}

data class ProjectMemberPageState(
    val mode: ManagerWorkerPageMode,
    val projectId: Long?,
) {
    val workerQueryProjectId: Long?
        get() = projectId

    val title: String
        get() = when (mode) {
            ManagerWorkerPageMode.GLOBAL -> "工人管理"
            ManagerWorkerPageMode.PROJECT -> "项目成员"
        }

    val loadingText: String
        get() = when (mode) {
            ManagerWorkerPageMode.GLOBAL -> "正在加载工人..."
            ManagerWorkerPageMode.PROJECT -> "正在加载项目成员..."
        }

    val emptyText: String
        get() = when (mode) {
            ManagerWorkerPageMode.GLOBAL -> "暂无工人"
            ManagerWorkerPageMode.PROJECT -> "暂无项目成员"
        }

    val filterEmptyText: String
        get() = "当前筛选下暂无成员"

    companion object {
        fun fromArgument(projectId: Long): ProjectMemberPageState {
            return if (projectId > 0L) {
                ProjectMemberPageState(ManagerWorkerPageMode.PROJECT, projectId)
            } else {
                ProjectMemberPageState(ManagerWorkerPageMode.GLOBAL, null)
            }
        }
    }
}

data class ManagerWorkerMemberSummary(
    val totalWorkers: Int,
    val teamLeaderCount: Int,
    val activeWorkers: Int,
    val enteringWorkers: Int,
    val exitedWorkers: Int,
    val abnormalWorkers: Int,
)

fun List<ManagerWorker>.toMemberSummary(): ManagerWorkerMemberSummary {
    val normalized = map { it.normalizedBindStatus }
    return ManagerWorkerMemberSummary(
        totalWorkers = size,
        teamLeaderCount = map { it.leaderId }.filter { it > 0L }.distinct().size,
        activeWorkers = normalized.count { it == "ACTIVE" },
        enteringWorkers = normalized.count { it == "ENTERING" || it == "BOUND" },
        exitedWorkers = normalized.count { it == "EXITED" || it == "LEFT" },
        abnormalWorkers = normalized.count { it == "CANCELLED" || it == "REJECTED" || it == "ABNORMAL" },
    )
}

fun List<ManagerWorker>.filterForMemberPage(filter: ManagerWorkerFilter): List<ManagerWorker> {
    val leaderIds = map { it.leaderId }.filter { it > 0L }.toSet()
    return when (filter) {
        ManagerWorkerFilter.ALL -> this
        ManagerWorkerFilter.TEAM_LEADER -> filter { it.workerUserId in leaderIds }
        ManagerWorkerFilter.WORKER -> filter { it.workerUserId !in leaderIds }
        ManagerWorkerFilter.ENTERING -> filter {
            it.normalizedBindStatus == "ENTERING" || it.normalizedBindStatus == "BOUND"
        }
        ManagerWorkerFilter.EXITED -> filter {
            it.normalizedBindStatus == "EXITED" || it.normalizedBindStatus == "LEFT"
        }
    }
}

val ManagerWorker.normalizedBindStatus: String
    get() = bindStatus.ifBlank { entryStatus }.uppercase(Locale.getDefault())

fun ManagerWorker.memberStatusText(): String {
    return when (normalizedBindStatus) {
        "ACTIVE" -> "在场"
        "BOUND", "ENTERING" -> "待入场"
        "EXITED", "LEFT" -> "已离场"
        "CANCELLED" -> "已取消"
        "REJECTED" -> "已驳回"
        "COMPLETED" -> "已完成"
        else -> normalizedBindStatus.ifBlank { "--" }
    }
}

data class ManagerTeamLeaderSummary(
    val leaderId: Long,
    val leaderName: String,
    val mobile: String = "",
    val projectCount: Int,
    val teamCount: Int,
    val workerCount: Int,
    val abnormalCount: Int,
    val scopes: List<ManagerTeamLeaderScope>,
)

data class ManagerTeamLeaderScope(
    val projectId: Long,
    val projectName: String,
    val teamId: Long,
    val teamName: String,
    val leaderId: Long,
    val leaderName: String,
    val workerCount: Int,
    val abnormalCount: Int,
)

fun toTeamLeaderSummaries(
    projects: List<ManagerProject>,
    teamsByProject: Map<Long, List<ManagerProjectTeam>>,
    workersByProject: Map<Long, List<ManagerWorker>>,
): List<ManagerTeamLeaderSummary> {
    val projectsById = projects.associateBy { it.projectId }
    val scopes = teamsByProject.flatMap { (projectId, teams) ->
        val projectName = projectsById[projectId]?.projectName.orEmpty()
        val projectWorkers = workersByProject[projectId].orEmpty()
        teams.filter { it.leaderId > 0L }.map { team ->
            val teamWorkers = projectWorkers.filter { worker ->
                worker.teamId == team.teamId || worker.leaderId == team.leaderId
            }
            ManagerTeamLeaderScope(
                projectId = projectId,
                projectName = projectName,
                teamId = team.teamId,
                teamName = team.teamName,
                leaderId = team.leaderId,
                leaderName = team.leaderName,
                workerCount = teamWorkers.size,
                abnormalCount = teamWorkers.count { it.normalizedBindStatus in ABNORMAL_BIND_STATUSES },
            )
        }
    }

    return scopes.groupBy { it.leaderId }.map { (leaderId, leaderScopes) ->
        val leaderWorkers = leaderScopes.flatMap { scope ->
            workersByProject[scope.projectId].orEmpty().filter { it.workerUserId == leaderId }
        }
        ManagerTeamLeaderSummary(
            leaderId = leaderId,
            leaderName = leaderScopes.firstNotNullOfOrNull { it.leaderName.takeIf(String::isNotBlank) }.orEmpty(),
            mobile = leaderWorkers.firstNotNullOfOrNull { it.mobile.takeIf(String::isNotBlank) }.orEmpty(),
            projectCount = leaderScopes.map { it.projectId }.distinct().size,
            teamCount = leaderScopes.map { it.teamId }.distinct().size,
            workerCount = leaderScopes.sumOf { it.workerCount },
            abnormalCount = leaderScopes.sumOf { it.abnormalCount },
            scopes = leaderScopes.sortedWith(compareBy<ManagerTeamLeaderScope> { it.projectName }.thenBy { it.teamName }),
        )
    }.sortedWith(compareByDescending<ManagerTeamLeaderSummary> { it.abnormalCount }.thenBy { it.leaderName })
}

private val ABNORMAL_BIND_STATUSES = setOf("CANCELLED", "REJECTED", "ABNORMAL")
