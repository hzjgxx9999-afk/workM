package com.qkzc.workerm.data.aiwarning

import com.qkzc.workerm.data.aiwarning.model.AiWarningDetail
import com.qkzc.workerm.data.aiwarning.model.AiWarningHandleStatus
import com.qkzc.workerm.data.aiwarning.model.AiWarningHazard
import com.qkzc.workerm.data.aiwarning.model.AiWarningItem
import com.qkzc.workerm.data.aiwarning.model.AiWarningPage
import com.qkzc.workerm.data.network.AiWarningDetailReq
import com.qkzc.workerm.data.network.AiWarningHandleReq
import com.qkzc.workerm.data.network.AiWarningHazardVo
import com.qkzc.workerm.data.network.AiWarningListReq
import com.qkzc.workerm.data.network.AiWarningPageVo
import com.qkzc.workerm.data.network.AiWarningReadReq
import com.qkzc.workerm.data.network.AiWarningVo
import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ApiClient
import com.qkzc.workerm.data.network.ApiException
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.network.bearerToken
import com.qkzc.workerm.data.network.requireSuccess

data class AiWarningFilter(
    val projectId: Long? = null,
    val readFlag: Int? = null,
    val handleStatus: String? = null,
    val riskLevel: String? = null,
    val pageNum: Int = 1,
    val pageSize: Int = 20,
)

class AiWarningRepository(
    private val api: SupervisorApi = ApiClient.supervisorApi,
) {

    suspend fun loadWarnings(
        token: String,
        filter: AiWarningFilter = AiWarningFilter(),
    ): AiWarningPage {
        val auth = bearerToken(token)
        val page = api.manageAiWarningList(
            token = auth,
            body = AiWarningListReq(
                projectId = filter.projectId,
                readFlag = filter.readFlag,
                handleStatus = filter.handleStatus,
                riskLevel = filter.riskLevel,
                pageNum = filter.pageNum,
                pageSize = filter.pageSize,
            ),
        ).requireData("AI预警列表为空")
        val unreadCount = api.manageAiWarningUnreadCount(
            token = auth,
            body = AiWarningListReq(projectId = filter.projectId),
        ).requireData("AI预警未读数为空")
        return page.toDomain(unreadCount)
    }

    suspend fun detail(token: String, warningId: Long): AiWarningDetail {
        val detail = api.manageAiWarningDetail(
            token = bearerToken(token),
            body = AiWarningDetailReq(warningId),
        ).requireData("AI预警详情为空")
        return detail.toDetail()
    }

    suspend fun markRead(token: String, warningId: Long) {
        api.manageAiWarningRead(
            token = bearerToken(token),
            body = AiWarningReadReq(warningId),
        ).requireOk()
    }

    suspend fun handle(token: String, warningId: Long, handleStatus: String) {
        require(handleStatus == AiWarningHandleStatus.HANDLED || handleStatus == AiWarningHandleStatus.IGNORED) {
            "不支持的处理状态：$handleStatus"
        }
        api.manageAiWarningHandle(
            token = bearerToken(token),
            body = AiWarningHandleReq(warningId = warningId, handleStatus = handleStatus),
        ).requireOk()
    }

    private fun AiWarningPageVo.toDomain(unreadCount: Int): AiWarningPage {
        val mappedRows = rows.orEmpty().map { row -> row.toItem() }
        return AiWarningPage(
            total = total ?: mappedRows.size.toLong(),
            unreadCount = unreadCount,
            pageNum = pageNum ?: 1,
            pageSize = pageSize ?: mappedRows.size.coerceAtLeast(20),
            pages = pages ?: 1,
            hasMore = hasMore ?: false,
            rows = mappedRows,
        )
    }

    private fun AiWarningVo.toDetail(): AiWarningDetail {
        return AiWarningDetail(
            item = toItem(),
            captureTime = captureTime.orEmpty(),
            aiModel = aiModel.orEmpty(),
            hazards = hazards.orEmpty().map { hazard -> hazard.toDomain() },
            needManualReview = needManualReview == true,
            manualReviewReason = manualReviewReason.orEmpty(),
            status = status.orEmpty(),
            errorMsg = errorMsg,
        )
    }

    private fun AiWarningVo.toItem(): AiWarningItem {
        return AiWarningItem(
            warningId = warningId ?: 0L,
            recordId = recordId,
            receiverRole = receiverRole.orEmpty(),
            workerName = workerName.orEmpty(),
            projectName = projectName.orEmpty(),
            leaderName = leaderName.orEmpty(),
            sceneName = sceneName.orEmpty(),
            inspectionPoint = inspectionPoint.orEmpty(),
            workType = workType.orEmpty(),
            photoUrl = photoUrl,
            riskLevel = riskLevel.orEmpty(),
            riskScore = riskScore,
            title = title?.takeIf { it.isNotBlank() } ?: "AI巡查预警",
            content = content.orEmpty(),
            summary = summary.orEmpty(),
            readFlag = readFlag == true,
            handleStatus = handleStatus?.takeIf { it.isNotBlank() } ?: AiWarningHandleStatus.PENDING,
            createTime = createTime.orEmpty(),
        )
    }

    private fun AiWarningHazardVo.toDomain(): AiWarningHazard {
        return AiWarningHazard(
            code = code.orEmpty(),
            name = name.orEmpty(),
            level = level.orEmpty(),
            confidence = confidence,
            evidence = evidence.orEmpty(),
            advice = advice.orEmpty(),
        )
    }

    private fun <T> AjaxResp<T>.requireData(emptyMessage: String): T {
        requireSuccess(code, msg)
        return data ?: throw ApiException(emptyMessage)
    }

    private fun AjaxResp<*>.requireOk() {
        requireSuccess(code, msg)
    }
}
