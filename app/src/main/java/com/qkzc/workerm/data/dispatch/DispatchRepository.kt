package com.qkzc.workerm.data.dispatch

import com.qkzc.workerm.data.network.ApiClient
import com.qkzc.workerm.data.network.bearerToken
import com.qkzc.workerm.data.network.requireSuccess

class DispatchRepository(private val api: DispatchApi = ApiClient.dispatchApi) {
    suspend fun page(token: String, status: String? = null, pageSize: Int = 20): List<DispatchOrder> {
        val response = api.page(bearerToken(token), status, null, 1, pageSize)
        requireSuccess(response.code, response.msg)
        return response.rows
    }

    suspend fun recent(token: String): List<DispatchOrder> = page(token, pageSize = 3)

    suspend fun detail(token: String, id: Long): DispatchDetail {
        val response = api.detail(bearerToken(token), id)
        requireSuccess(response.code, response.msg)
        return response.data ?: DispatchDetail()
    }

    suspend fun acceptanceDetail(token: String, id: Long): DispatchAcceptanceDetail {
        val response = api.acceptanceDetail(bearerToken(token), id)
        requireSuccess(response.code, response.msg)
        return response.data ?: DispatchAcceptanceDetail()
    }

    suspend fun create(token: String, body: DispatchCreateReq): DispatchOrder {
        val response = api.create(bearerToken(token), body)
        requireSuccess(response.code, response.msg)
        return response.data ?: DispatchOrder()
    }

    suspend fun createManualProcessNode(token: String, id: Long, body: DispatchManualProcessNodeReq): DispatchOrder {
        val response = api.createManualProcessNode(bearerToken(token), id, body)
        requireSuccess(response.code, response.msg)
        return response.data ?: DispatchOrder()
    }

    suspend fun createSpotCheck(token: String, id: Long, body: DispatchManualProcessNodeReq): DispatchOrder {
        val response = api.createSpotCheck(bearerToken(token), id, body)
        requireSuccess(response.code, response.msg)
        return response.data ?: DispatchOrder()
    }

    suspend fun spotCheckDetail(token: String, id: Long, nodeId: Long): DispatchSpotCheckDetail {
        val response = api.spotCheckDetail(bearerToken(token), id, nodeId)
        requireSuccess(response.code, response.msg)
        return response.data ?: DispatchSpotCheckDetail()
    }

    suspend fun cancelSpotCheck(token: String, id: Long, nodeId: Long, reason: String): DispatchOrder {
        val response = api.cancelSpotCheck(bearerToken(token), id, nodeId, DispatchActionReq(0, reason))
        requireSuccess(response.code, response.msg)
        return response.data ?: DispatchOrder()
    }

    suspend fun reviewProcessRecord(token: String, recordId: Long, result: String, comment: String, version: Int): DispatchOrder {
        val response = api.reviewProcessRecord(bearerToken(token), recordId, DispatchProcessReviewReq(result, comment, version))
        requireSuccess(response.code, response.msg)
        return response.data ?: DispatchOrder()
    }

    suspend fun approve(token: String, id: Long, version: Int, remark: String? = null): DispatchOrder {
        val response = api.approve(bearerToken(token), id, DispatchActionReq(version, remark))
        requireSuccess(response.code, response.msg)
        return response.data ?: DispatchOrder()
    }

    suspend fun reject(token: String, id: Long, version: Int, remark: String): DispatchOrder {
        val response = api.reject(bearerToken(token), id, DispatchActionReq(version, remark))
        requireSuccess(response.code, response.msg)
        return response.data ?: DispatchOrder()
    }

    suspend fun cancel(token: String, id: Long, version: Int, remark: String): DispatchOrder {
        val response = api.cancel(bearerToken(token), id, DispatchActionReq(version, remark))
        requireSuccess(response.code, response.msg)
        return response.data ?: DispatchOrder()
    }

    suspend fun reassign(token: String, id: Long, teamId: Long, leaderId: Long, version: Int): DispatchOrder {
        val response = api.reassign(bearerToken(token), id, DispatchReassignReq(teamId, leaderId, version))
        requireSuccess(response.code, response.msg)
        return response.data ?: DispatchOrder()
    }
}
