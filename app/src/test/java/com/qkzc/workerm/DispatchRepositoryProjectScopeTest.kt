package com.qkzc.workerm

import com.qkzc.workerm.data.dispatch.DispatchActionReq
import com.qkzc.workerm.data.dispatch.DispatchApi
import com.qkzc.workerm.data.dispatch.DispatchAcceptanceDetail
import com.qkzc.workerm.data.dispatch.DispatchCreateReq
import com.qkzc.workerm.data.dispatch.DispatchDetail
import com.qkzc.workerm.data.dispatch.DispatchManualProcessNodeReq
import com.qkzc.workerm.data.dispatch.DispatchOrder
import com.qkzc.workerm.data.dispatch.DispatchProcessReviewReq
import com.qkzc.workerm.data.dispatch.DispatchReassignReq
import com.qkzc.workerm.data.dispatch.DispatchRepository
import com.qkzc.workerm.data.dispatch.DispatchSpotCheckDetail
import com.qkzc.workerm.data.dispatch.DispatchTableResp
import com.qkzc.workerm.data.network.AjaxResp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DispatchRepositoryProjectScopeTest {

    @Test
    fun pagePassesProjectStatusKeywordPageSizeAndLatestSort() = runBlocking {
        val api = FakeDispatchApi()
        val repository = DispatchRepository(api)

        val orders = repository.page(
            token = "token",
            projectId = 1001L,
            status = "IN_PROGRESS",
            keyword = "repair",
            pageSize = 30,
        )

        assertEquals(listOf(DispatchOrder(id = 1L, projectId = 1001L)), orders)
        assertEquals("Bearer token", api.lastToken)
        assertEquals(1001L, api.lastProjectId)
        assertEquals("IN_PROGRESS", api.lastStatus)
        assertEquals("repair", api.lastKeyword)
        assertEquals(1, api.lastPageNum)
        assertEquals(30, api.lastPageSize)
        assertEquals("createTime", api.lastOrderByColumn)
        assertEquals("desc", api.lastIsAsc)
    }

    @Test
    fun recentUsesProjectScopeFiveRowsAndLatestSortByDefault() = runBlocking {
        val api = FakeDispatchApi()
        val repository = DispatchRepository(api)

        api.responseRows = (1L..7L).map { DispatchOrder(id = it, projectId = 8L) } +
            (8L..12L).map { DispatchOrder(id = it, projectId = 99L) }
        val orders = repository.recent(token = "token", projectId = 8L)

        assertEquals(8L, api.lastProjectId)
        assertEquals(5, api.lastPageSize)
        assertEquals("createTime", api.lastOrderByColumn)
        assertEquals("desc", api.lastIsAsc)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), orders.map { it.id })
        assertEquals(listOf(8L, 8L, 8L, 8L, 8L), orders.map { it.projectId })
    }

    private class FakeDispatchApi : EmptyDispatchApi() {
        var lastToken: String? = null
        var lastProjectId: Long? = null
        var lastStatus: String? = null
        var lastKeyword: String? = null
        var lastPageNum: Int? = null
        var lastPageSize: Int? = null
        var lastOrderByColumn: String? = null
        var lastIsAsc: String? = null
        var responseRows: List<DispatchOrder>? = null

        override suspend fun page(
            token: String,
            projectId: Long?,
            status: String?,
            keyword: String?,
            pageNum: Int,
            pageSize: Int,
            orderByColumn: String?,
            isAsc: String?,
        ): DispatchTableResp<DispatchOrder> {
            lastToken = token
            lastProjectId = projectId
            lastStatus = status
            lastKeyword = keyword
            lastPageNum = pageNum
            lastPageSize = pageSize
            lastOrderByColumn = orderByColumn
            lastIsAsc = isAsc
            return DispatchTableResp(
                code = 200,
                msg = "ok",
                rows = responseRows ?: listOf(DispatchOrder(id = 1L, projectId = projectId ?: 0L)),
            )
        }
    }

    private open class EmptyDispatchApi : DispatchApi {
        override suspend fun page(
            token: String,
            projectId: Long?,
            status: String?,
            keyword: String?,
            pageNum: Int,
            pageSize: Int,
            orderByColumn: String?,
            isAsc: String?,
        ): DispatchTableResp<DispatchOrder> = error("unused")

        override suspend fun detail(token: String, id: Long): AjaxResp<DispatchDetail> = error("unused")
        override suspend fun acceptanceDetail(token: String, id: Long): AjaxResp<DispatchAcceptanceDetail> = error("unused")
        override suspend fun create(token: String, body: DispatchCreateReq): AjaxResp<DispatchOrder> = error("unused")
        override suspend fun createManualProcessNode(token: String, id: Long, body: DispatchManualProcessNodeReq): AjaxResp<DispatchOrder> = error("unused")
        override suspend fun createSpotCheck(token: String, id: Long, body: DispatchManualProcessNodeReq): AjaxResp<DispatchOrder> = error("unused")
        override suspend fun spotCheckDetail(token: String, id: Long, nodeId: Long): AjaxResp<DispatchSpotCheckDetail> = error("unused")
        override suspend fun cancelSpotCheck(token: String, id: Long, nodeId: Long, body: DispatchActionReq): AjaxResp<DispatchOrder> = error("unused")
        override suspend fun reviewProcessRecord(token: String, recordId: Long, body: DispatchProcessReviewReq): AjaxResp<DispatchOrder> = error("unused")
        override suspend fun approve(token: String, id: Long, body: DispatchActionReq): AjaxResp<DispatchOrder> = error("unused")
        override suspend fun reject(token: String, id: Long, body: DispatchActionReq): AjaxResp<DispatchOrder> = error("unused")
        override suspend fun cancel(token: String, id: Long, body: DispatchActionReq): AjaxResp<DispatchOrder> = error("unused")
        override suspend fun reassign(token: String, id: Long, body: DispatchReassignReq): AjaxResp<DispatchOrder> = error("unused")
    }
}
