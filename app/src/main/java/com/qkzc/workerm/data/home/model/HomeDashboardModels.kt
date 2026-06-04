package com.qkzc.workerm.data.home.model

data class ApiEnvelope<T>(
    val code: Int,
    val message: String,
    val data: T?,
)

data class HomeDashboardRequest(
    val projectId: String,
    val organizationId: String? = null,
    val queryDate: String? = null,
    val includeWarningList: Boolean = true,
    val includePendingList: Boolean = true,
    val includeTrendSection: Boolean = true,
)

data class HomeDashboardResponse(
    val project: HomeProjectContextDto,
    val statistics: List<HomeStatisticDto>,
    val warnings: List<HomeWarningDto>,
    val pendingTasks: List<HomePendingTaskDto>,
    val quickEntries: List<HomeQuickEntryDto>,
    val trends: List<HomeTrendDto>,
    val ranking: List<HomeProjectRankingDto>,
)

data class HomeProjectContextDto(
    val projectId: String,
    val projectName: String,
    val organizationName: String,
    val sectionName: String? = null,
)

data class HomeStatisticDto(
    val code: String,
    val label: String,
    val value: String,
    val unit: String? = null,
    val trendText: String? = null,
    val warningLevel: String? = null,
)

data class HomeWarningDto(
    val warningId: String,
    val title: String,
    val content: String,
    val level: String,
    val sourceType: String,
    val status: String,
    val happenedAt: String,
)

data class HomePendingTaskDto(
    val taskId: String,
    val title: String,
    val content: String,
    val taskType: String,
    val count: Int,
    val deadline: String? = null,
)

data class HomeQuickEntryDto(
    val entryCode: String,
    val entryName: String,
    val route: String,
    val permissionCode: String? = null,
)

data class HomeTrendDto(
    val trendCode: String,
    val trendName: String,
    val values: List<HomeTrendPointDto>,
)

data class HomeTrendPointDto(
    val label: String,
    val value: Float,
)

data class HomeProjectRankingDto(
    val projectId: String,
    val projectName: String,
    val rank: Int,
    val score: Float,
)

data class HomeDashboard(
    val projectName: String,
    val organizationName: String,
    val statistics: List<HomeStatistic>,
    val warnings: List<HomeWarning>,
    val pendingTasks: List<HomePendingTask>,
    val quickEntries: List<HomeQuickEntry>,
    val trends: List<HomeTrend>,
    val ranking: List<HomeProjectRanking>,
)

data class HomeStatistic(
    val code: String,
    val label: String,
    val value: String,
    val unit: String?,
    val trendText: String?,
    val warningLevel: String?,
)

data class HomeWarning(
    val id: String,
    val title: String,
    val content: String,
    val level: String,
    val sourceType: String,
    val status: String,
    val happenedAt: String,
)

data class HomePendingTask(
    val id: String,
    val title: String,
    val content: String,
    val taskType: String,
    val count: Int,
    val deadline: String?,
)

data class HomeQuickEntry(
    val code: String,
    val name: String,
    val route: String,
    val permissionCode: String?,
)

data class HomeTrend(
    val code: String,
    val name: String,
    val points: List<HomeTrendPoint>,
)

data class HomeTrendPoint(
    val label: String,
    val value: Float,
)

data class HomeProjectRanking(
    val projectId: String,
    val projectName: String,
    val rank: Int,
    val score: Float,
)

fun HomeDashboardResponse.toDomain(): HomeDashboard {
    return HomeDashboard(
        projectName = project.projectName,
        organizationName = project.organizationName,
        statistics = statistics.map {
            HomeStatistic(
                code = it.code,
                label = it.label,
                value = it.value,
                unit = it.unit,
                trendText = it.trendText,
                warningLevel = it.warningLevel,
            )
        },
        warnings = warnings.map {
            HomeWarning(
                id = it.warningId,
                title = it.title,
                content = it.content,
                level = it.level,
                sourceType = it.sourceType,
                status = it.status,
                happenedAt = it.happenedAt,
            )
        },
        pendingTasks = pendingTasks.map {
            HomePendingTask(
                id = it.taskId,
                title = it.title,
                content = it.content,
                taskType = it.taskType,
                count = it.count,
                deadline = it.deadline,
            )
        },
        quickEntries = quickEntries.map {
            HomeQuickEntry(
                code = it.entryCode,
                name = it.entryName,
                route = it.route,
                permissionCode = it.permissionCode,
            )
        },
        trends = trends.map { trend ->
            HomeTrend(
                code = trend.trendCode,
                name = trend.trendName,
                points = trend.values.map { point ->
                    HomeTrendPoint(label = point.label, value = point.value)
                },
            )
        },
        ranking = ranking.map {
            HomeProjectRanking(
                projectId = it.projectId,
                projectName = it.projectName,
                rank = it.rank,
                score = it.score,
            )
        },
    )
}
