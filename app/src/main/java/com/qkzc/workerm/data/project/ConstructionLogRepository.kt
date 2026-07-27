package com.qkzc.workerm.data.project

import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ApiClient
import com.qkzc.workerm.data.network.ConstructionLogAuditReq
import com.qkzc.workerm.data.network.ConstructionLogAttachmentSaveReq
import com.qkzc.workerm.data.network.ConstructionLogAttachmentVo
import com.qkzc.workerm.data.network.ConstructionLogAuditNodeVo
import com.qkzc.workerm.data.network.ConstructionLogDetailVo
import com.qkzc.workerm.data.network.ConstructionLogItemVo
import com.qkzc.workerm.data.network.ConstructionLogOverviewVo
import com.qkzc.workerm.data.network.ConstructionLogPageReq
import com.qkzc.workerm.data.network.ConstructionLogSaveReq
import com.qkzc.workerm.data.network.ConstructionLogVersionVo
import com.qkzc.workerm.data.network.FileUploadVo
import com.qkzc.workerm.data.network.PageResp
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.network.VersionReq
import com.qkzc.workerm.data.network.bearerToken
import com.qkzc.workerm.data.network.requireSuccess
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class ConstructionLogRepository(
    private val api: SupervisorApi = ApiClient.supervisorApi,
) {

    suspend fun loadOverview(token: String, projectId: Long): ConstructionLogOverview {
        val response = api.constructionLogOverview(bearerToken(token), projectId)
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: ConstructionLogOverview()
    }

    suspend fun loadPage(
        token: String,
        projectId: Long,
        pageNum: Int = 1,
        pageSize: Int = 20,
        keyword: String? = null,
        status: String? = null,
        dateStart: String? = null,
        dateEnd: String? = null,
        teamId: Long? = null,
        mineOnly: Boolean? = null,
    ): ConstructionLogPage {
        val response = api.constructionLogPage(
            token = bearerToken(token),
            projectId = projectId,
            body = ConstructionLogPageReq(
                pageNum = pageNum,
                pageSize = pageSize,
                keyword = keyword?.takeIf { it.isNotBlank() },
                status = status?.takeIf { it.isNotBlank() },
                dateStart = dateStart?.takeIf { it.isNotBlank() },
                dateEnd = dateEnd?.takeIf { it.isNotBlank() },
                teamId = teamId,
                mineOnly = mineOnly,
            ),
        )
        requireSuccess(response.code, response.msg)
        val page = response.data ?: PageResp<ConstructionLogItemVo>()
        return ConstructionLogPage(
            total = page.total ?: 0L,
            pageNum = page.pageNum ?: pageNum,
            pageSize = page.pageSize ?: pageSize,
            rows = page.rows.orEmpty().map { it.toDomain() },
        )
    }

    suspend fun detail(token: String, projectId: Long, logId: Long): ConstructionLogDetail {
        val response = api.constructionLogDetail(bearerToken(token), projectId, logId)
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: error("日志详情不存在")
    }

    suspend fun create(token: String, projectId: Long, draft: ConstructionLogDraft): ConstructionLogDetail {
        val response = api.createConstructionLog(
            bearerToken(token),
            projectId,
            draft.toSaveReq(),
        )
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: error("日志保存成功但未返回详情")
    }

    suspend fun update(
        token: String,
        projectId: Long,
        logId: Long,
        draft: ConstructionLogDraft,
    ): ConstructionLogDetail {
        val response = api.updateConstructionLog(
            bearerToken(token),
            projectId,
            logId,
            draft.toSaveReq(),
        )
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: error("日志更新成功但未返回详情")
    }

    suspend fun delete(token: String, projectId: Long, logId: Long, version: Int) {
        val response = api.deleteConstructionLog(
            bearerToken(token),
            projectId,
            logId,
            VersionReq(version),
        )
        requireSuccess(response.code, response.msg)
    }

    suspend fun submit(token: String, projectId: Long, logId: Long, version: Int) {
        val response = api.submitConstructionLog(
            bearerToken(token),
            projectId,
            logId,
            VersionReq(version),
        )
        requireSuccess(response.code, response.msg)
    }

    suspend fun withdraw(token: String, projectId: Long, logId: Long, version: Int) {
        val response = api.withdrawConstructionLog(
            bearerToken(token),
            projectId,
            logId,
            VersionReq(version),
        )
        requireSuccess(response.code, response.msg)
    }

    suspend fun approve(token: String, projectId: Long, logId: Long, version: Int, remark: String? = null): ConstructionLogDetail {
        val response = api.approveConstructionLog(
            bearerToken(token),
            projectId,
            logId,
            ConstructionLogAuditReq(version, remark?.takeIf { it.isNotBlank() }),
        )
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: error("审核成功但未返回日志详情")
    }

    suspend fun reject(token: String, projectId: Long, logId: Long, version: Int, remark: String): ConstructionLogDetail {
        val response = api.rejectConstructionLog(
            bearerToken(token),
            projectId,
            logId,
            ConstructionLogAuditReq(version, remark.takeIf { it.isNotBlank() }),
        )
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: error("驳回成功但未返回日志详情")
    }

    suspend fun uploadFile(
        token: String,
        fileName: String,
        mimeType: String?,
        bytes: ByteArray,
    ): ConstructionLogAttachment {
        val requestBody = bytes.toRequestBody((mimeType ?: "application/octet-stream").toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", fileName, requestBody)
        val response = api.uploadCommonFile(bearerToken(token), filePart)
        requireSuccess(response.code, response.msg)
        return response.data?.toUploadDomain() ?: error("附件上传成功但未返回文件信息")
    }

    private fun ConstructionLogDraft.toSaveReq(): ConstructionLogSaveReq {
        return ConstructionLogSaveReq(
            requestId = requestId.ifBlank { UUID.randomUUID().toString() },
            logDate = logDate,
            logType = logType,
            sectionId = sectionId,
            sectionName = sectionName.takeIf { it.isNotBlank() },
            teamId = teamId,
            teamName = teamName.takeIf { it.isNotBlank() },
            title = title,
            weatherAm = weatherAm.takeIf { it.isNotBlank() },
            weatherPm = weatherPm.takeIf { it.isNotBlank() },
            temperatureMin = temperatureMin,
            temperatureMax = temperatureMax,
            workerCount = workerCount,
            machineCount = machineCount,
            progressSummary = progressSummary,
            qualitySummary = qualitySummary,
            safetySummary = safetySummary,
            contentHtml = contentHtml.takeIf { it.isNotBlank() },
            contentText = contentText,
            riskCount = riskCount,
            locationText = locationText.takeIf { it.isNotBlank() },
            rectifySuggestion = rectifySuggestion.takeIf { it.isNotBlank() },
            extraRemark = extraRemark.takeIf { it.isNotBlank() },
            attachments = attachments.mapIndexed { index, item ->
                ConstructionLogAttachmentSaveReq(
                    fileCategory = item.fileCategory,
                    fileName = item.fileName,
                    objectKey = item.objectKey,
                    objectUrl = item.objectUrl.takeIf { it.isNotBlank() },
                    mimeType = item.mimeType.takeIf { it.isNotBlank() },
                    fileSize = item.fileSize,
                    sha256 = item.sha256.takeIf { it.isNotBlank() },
                    sortNo = index,
                )
            },
            version = version,
        )
    }

    private fun ConstructionLogOverviewVo.toDomain(): ConstructionLogOverview {
        return ConstructionLogOverview(
            todayCount = todayCount ?: 0,
            pendingCount = pendingCount ?: 0,
            archivedMonthCount = archivedMonthCount ?: 0,
            draftCount = draftCount ?: 0,
        )
    }

    private fun ConstructionLogItemVo.toDomain(): ConstructionLogItem {
        return ConstructionLogItem(
            id = id ?: 0L,
            logNo = logNo.orEmpty(),
            logDate = logDate.orEmpty(),
            title = title.orEmpty(),
            logType = logType.orEmpty(),
            teamId = teamId,
            teamName = teamName.orEmpty(),
            recorderName = recorderName.orEmpty(),
            workerCount = workerCount ?: 0,
            machineCount = machineCount ?: 0,
            status = status.orEmpty(),
            statusName = statusName.orEmpty(),
            currentAuditorName = currentAuditorName.orEmpty(),
            attachmentCount = attachmentCount ?: 0,
            submittedAt = submittedAt.orEmpty(),
            updateTime = updateTime.orEmpty(),
            actionCodes = actionCodes.orEmpty(),
        )
    }

    private fun ConstructionLogDetailVo.toDomain(): ConstructionLogDetail {
        return ConstructionLogDetail(
            id = id ?: 0L,
            logNo = logNo.orEmpty(),
            projectId = projectId ?: 0L,
            projectName = projectName.orEmpty(),
            logDate = logDate.orEmpty(),
            logType = logType.orEmpty(),
            sectionId = sectionId,
            sectionName = sectionName.orEmpty(),
            teamId = teamId,
            teamName = teamName.orEmpty(),
            title = title.orEmpty(),
            weatherAm = weatherAm.orEmpty(),
            weatherPm = weatherPm.orEmpty(),
            temperatureMin = temperatureMin,
            temperatureMax = temperatureMax,
            workerCount = workerCount ?: 0,
            machineCount = machineCount ?: 0,
            progressSummary = progressSummary.orEmpty(),
            qualitySummary = qualitySummary.orEmpty(),
            safetySummary = safetySummary.orEmpty(),
            contentHtml = contentHtml.orEmpty(),
            contentText = contentText.orEmpty(),
            riskCount = riskCount ?: 0,
            locationText = locationText.orEmpty(),
            rectifySuggestion = rectifySuggestion.orEmpty(),
            extraRemark = extraRemark.orEmpty(),
            status = status.orEmpty(),
            statusName = statusName.orEmpty(),
            version = version ?: 0,
            recorderName = recorderName.orEmpty(),
            submitterName = submitterName.orEmpty(),
            currentAuditorName = currentAuditorName.orEmpty(),
            submittedAt = submittedAt.orEmpty(),
            approvedAt = approvedAt.orEmpty(),
            attachments = attachments.orEmpty().map { it.toDomain() },
            auditTracks = auditTracks.orEmpty().map { it.toDomain() },
            versions = versions.orEmpty().map { it.toDomain() },
            actionCodes = actionCodes.orEmpty(),
        )
    }

    private fun ConstructionLogAttachmentVo.toDomain(): ConstructionLogAttachment {
        return ConstructionLogAttachment(
            id = id,
            fileCategory = fileCategory.orEmpty().ifBlank { "DOCUMENT" },
            fileName = fileName.orEmpty(),
            mimeType = mimeType.orEmpty(),
            fileSize = fileSize ?: 0L,
            previewUrl = previewUrl.orEmpty(),
            downloadUrl = downloadUrl.orEmpty(),
            objectKey = objectKey.orEmpty(),
            objectUrl = objectUrl.orEmpty(),
            sha256 = sha256.orEmpty(),
            sortNo = sortNo ?: 0,
        )
    }

    private fun FileUploadVo.toUploadDomain(): ConstructionLogAttachment {
        return ConstructionLogAttachment(
            id = null,
            fileCategory = "DOCUMENT",
            fileName = fileName.orEmpty(),
            mimeType = mimeType.orEmpty(),
            fileSize = fileSize ?: 0L,
            previewUrl = fileUrl.orEmpty(),
            downloadUrl = fileUrl.orEmpty(),
            objectKey = objectKey.orEmpty(),
            objectUrl = fileUrl.orEmpty(),
            sha256 = sha256.orEmpty(),
            sortNo = 0,
            localOnly = false,
            fileToken = fileToken.orEmpty(),
        )
    }

    private fun ConstructionLogAuditNodeVo.toDomain(): ConstructionLogAuditNode {
        return ConstructionLogAuditNode(
            nodeNo = nodeNo ?: 0,
            nodeType = nodeType.orEmpty(),
            approverName = approverName.orEmpty(),
            action = action.orEmpty(),
            actionRemark = actionRemark.orEmpty(),
            status = status.orEmpty(),
            actedAt = actedAt.orEmpty(),
            createTime = createTime.orEmpty(),
        )
    }

    private fun ConstructionLogVersionVo.toDomain(): ConstructionLogVersion {
        return ConstructionLogVersion(
            versionNo = versionNo ?: 0,
            sourceAction = sourceAction.orEmpty(),
            operatorName = operatorName.orEmpty(),
            createTime = createTime.orEmpty(),
            diffSummary = diffSummary.orEmpty(),
        )
    }
}

data class ConstructionLogOverview(
    val todayCount: Int = 0,
    val pendingCount: Int = 0,
    val archivedMonthCount: Int = 0,
    val draftCount: Int = 0,
)

data class ConstructionLogPage(
    val total: Long = 0L,
    val pageNum: Int = 1,
    val pageSize: Int = 20,
    val rows: List<ConstructionLogItem> = emptyList(),
)

data class ConstructionLogItem(
    val id: Long,
    val logNo: String,
    val logDate: String,
    val title: String,
    val logType: String,
    val teamId: Long?,
    val teamName: String,
    val recorderName: String,
    val workerCount: Int,
    val machineCount: Int,
    val status: String,
    val statusName: String,
    val currentAuditorName: String,
    val attachmentCount: Int,
    val submittedAt: String,
    val updateTime: String,
    val actionCodes: List<String>,
)

data class ConstructionLogDetail(
    val id: Long,
    val logNo: String,
    val projectId: Long,
    val projectName: String,
    val logDate: String,
    val logType: String,
    val sectionId: Long?,
    val sectionName: String,
    val teamId: Long?,
    val teamName: String,
    val title: String,
    val weatherAm: String,
    val weatherPm: String,
    val temperatureMin: Int?,
    val temperatureMax: Int?,
    val workerCount: Int,
    val machineCount: Int,
    val progressSummary: String,
    val qualitySummary: String,
    val safetySummary: String,
    val contentHtml: String,
    val contentText: String,
    val riskCount: Int,
    val locationText: String,
    val rectifySuggestion: String,
    val extraRemark: String,
    val status: String,
    val statusName: String,
    val version: Int,
    val recorderName: String,
    val submitterName: String,
    val currentAuditorName: String,
    val submittedAt: String,
    val approvedAt: String,
    val attachments: List<ConstructionLogAttachment>,
    val auditTracks: List<ConstructionLogAuditNode>,
    val versions: List<ConstructionLogVersion>,
    val actionCodes: List<String>,
)

data class ConstructionLogAttachment(
    val id: Long? = null,
    val fileCategory: String = "DOCUMENT",
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val previewUrl: String,
    val downloadUrl: String,
    val objectKey: String,
    val objectUrl: String,
    val sha256: String,
    val sortNo: Int,
    val localOnly: Boolean = false,
    val fileToken: String = "",
) {
    val displaySize: String
        get() = if (fileSize <= 0L) "--" else "%.1f KB".format(fileSize / 1024f)
}

data class ConstructionLogAuditNode(
    val nodeNo: Int,
    val nodeType: String,
    val approverName: String,
    val action: String,
    val actionRemark: String,
    val status: String,
    val actedAt: String,
    val createTime: String,
)

data class ConstructionLogVersion(
    val versionNo: Int,
    val sourceAction: String,
    val operatorName: String,
    val createTime: String,
    val diffSummary: String,
)

data class ConstructionLogDraft(
    val requestId: String = UUID.randomUUID().toString(),
    val logDate: String,
    val logType: String = "DAILY",
    val sectionId: Long? = null,
    val sectionName: String = "",
    val teamId: Long? = null,
    val teamName: String = "",
    val title: String,
    val weatherAm: String = "",
    val weatherPm: String = "",
    val temperatureMin: Int? = null,
    val temperatureMax: Int? = null,
    val workerCount: Int = 0,
    val machineCount: Int = 0,
    val progressSummary: String,
    val qualitySummary: String,
    val safetySummary: String,
    val contentHtml: String = "",
    val contentText: String,
    val riskCount: Int = 0,
    val locationText: String = "",
    val rectifySuggestion: String = "",
    val extraRemark: String = "",
    val attachments: List<ConstructionLogAttachment> = emptyList(),
    val version: Int? = null,
)

private fun <T> AjaxResp<List<T>>.requireDataList(): List<T> {
    requireSuccess(code, msg)
    return data.orEmpty()
}
