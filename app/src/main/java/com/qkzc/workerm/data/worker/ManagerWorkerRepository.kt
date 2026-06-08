package com.qkzc.workerm.data.worker

import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ApiClient
import com.qkzc.workerm.data.network.ManagerWorkerScanReq
import com.qkzc.workerm.data.network.ManagerWorkerVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.network.bearerToken
import com.qkzc.workerm.data.network.requireSuccess

class ManagerWorkerRepository(
    private val api: SupervisorApi = ApiClient.supervisorApi,
) {
    suspend fun listWorkers(
        token: String,
        projectId: Long?,
        status: String? = null,
        keyword: String? = null,
    ): List<ManagerWorker> {
        return api.manageWorkers(
            token = bearerToken(token),
            projectId = projectId,
            status = status,
            keyword = keyword,
        ).requireDataList().map { it.toDomain() }
    }

    suspend fun detail(token: String, projectId: Long, workerUserId: Long): ManagerWorker {
        val response = api.manageWorkerDetail(
            token = bearerToken(token),
            workerId = workerUserId,
            projectId = projectId,
        )
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: ManagerWorker(workerUserId = workerUserId, projectId = projectId)
    }

    suspend fun scanTicket(token: String, projectId: Long, workerUserId: Long): ManagerWorker {
        val response = api.manageWorkerScanTicket(
            token = bearerToken(token),
            body = ManagerWorkerScanReq(projectId = projectId, workerUserId = workerUserId),
        )
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: ManagerWorker(workerUserId = workerUserId, projectId = projectId)
    }

    suspend fun scanQrTicket(token: String, projectId: Long, ticket: String): ManagerWorker {
        require(ticket.isNotBlank()) { "二维码ticket不能为空" }
        val response = api.manageWorkerScanTicket(
            token = bearerToken(token),
            body = ManagerWorkerScanReq(projectId = projectId, ticket = ticket),
        )
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: ManagerWorker(workerUserId = 0L, projectId = projectId)
    }

    private fun ManagerWorkerVo.toDomain(): ManagerWorker {
        return ManagerWorker(
            relationId = relationId ?: 0L,
            entryId = entryId ?: 0L,
            workerUserId = workerUserId ?: 0L,
            realName = realName.orEmpty(),
            mobile = mobile.orEmpty(),
            idCardNo = idCardNo.orEmpty(),
            userStatus = userStatus,
            workTypeName = workTypeName.orEmpty(),
            projectId = projectId ?: 0L,
            projectName = projectName.orEmpty(),
            leaderId = leaderId ?: 0L,
            leaderName = leaderName.orEmpty(),
            teamId = teamId ?: 0L,
            teamName = teamName.orEmpty(),
            bindStatus = bindStatus.orEmpty(),
            bindTime = bindTime.orEmpty(),
            enterTime = enterTime.orEmpty(),
            exitTime = exitTime.orEmpty(),
            entryStatus = entryStatus.orEmpty(),
            identityStatus = identityStatus.orEmpty(),
            safetyTrainingStatus = safetyTrainingStatus.orEmpty(),
            healthCheckStatus = healthCheckStatus.orEmpty(),
            entryContractStatus = entryContractStatus.orEmpty(),
            insuranceStatus = insuranceStatus.orEmpty(),
            signed = signed,
            signedTime = signedTime.orEmpty(),
            contractStatus = contractStatus.orEmpty(),
            createTime = createTime.orEmpty(),
        )
    }

    private fun <T> AjaxResp<List<T>>.requireDataList(): List<T> {
        requireSuccess(code, msg)
        return data.orEmpty()
    }
}

data class ManagerWorker(
    val relationId: Long = 0L,
    val entryId: Long = 0L,
    val workerUserId: Long,
    val realName: String = "",
    val mobile: String = "",
    val idCardNo: String = "",
    val userStatus: Long? = null,
    val workTypeName: String = "",
    val projectId: Long,
    val projectName: String = "",
    val leaderId: Long = 0L,
    val leaderName: String = "",
    val teamId: Long = 0L,
    val teamName: String = "",
    val bindStatus: String = "",
    val bindTime: String = "",
    val enterTime: String = "",
    val exitTime: String = "",
    val entryStatus: String = "",
    val identityStatus: String = "",
    val safetyTrainingStatus: String = "",
    val healthCheckStatus: String = "",
    val entryContractStatus: String = "",
    val insuranceStatus: String = "",
    val signed: Long? = null,
    val signedTime: String = "",
    val contractStatus: String = "",
    val createTime: String = "",
)
