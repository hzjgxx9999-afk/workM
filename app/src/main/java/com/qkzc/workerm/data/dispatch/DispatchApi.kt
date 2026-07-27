package com.qkzc.workerm.data.dispatch

import com.qkzc.workerm.data.network.AjaxResp
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DispatchApi {
    @GET("/app/manage/dispatch-orders/page")
    suspend fun page(
        @Header("Authorization") token: String,
        @Query("projectId") projectId: Long? = null,
        @Query("status") status: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("orderByColumn") orderByColumn: String? = null,
        @Query("isAsc") isAsc: String? = null,
    ): DispatchTableResp<DispatchOrder>
    @GET("/app/manage/dispatch-orders/{id}")
    suspend fun detail(@Header("Authorization") token: String, @Path("id") id: Long): AjaxResp<DispatchDetail>
    @GET("/app/manage/dispatch-orders/{id}/acceptance-detail")
    suspend fun acceptanceDetail(@Header("Authorization") token: String, @Path("id") id: Long): AjaxResp<DispatchAcceptanceDetail>
    @POST("/app/manage/dispatch-orders")
    suspend fun create(@Header("Authorization") token: String, @Body body: DispatchCreateReq): AjaxResp<DispatchOrder>
    @POST("/app/manage/dispatch-orders/{id}/process-nodes/manual")
    suspend fun createManualProcessNode(@Header("Authorization") token: String, @Path("id") id: Long, @Body body: DispatchManualProcessNodeReq): AjaxResp<DispatchOrder>
    @POST("/app/manage/dispatch-orders/{id}/spot-checks")
    suspend fun createSpotCheck(@Header("Authorization") token: String, @Path("id") id: Long, @Body body: DispatchManualProcessNodeReq): AjaxResp<DispatchOrder>
    @GET("/app/manage/dispatch-orders/{id}/spot-checks/{nodeId}")
    suspend fun spotCheckDetail(@Header("Authorization") token: String, @Path("id") id: Long, @Path("nodeId") nodeId: Long): AjaxResp<DispatchSpotCheckDetail>
    @POST("/app/manage/dispatch-orders/{id}/spot-checks/{nodeId}/cancel")
    suspend fun cancelSpotCheck(@Header("Authorization") token: String, @Path("id") id: Long, @Path("nodeId") nodeId: Long, @Body body: DispatchActionReq): AjaxResp<DispatchOrder>
    @POST("/app/manage/dispatch-orders/process-records/{recordId}/review")
    suspend fun reviewProcessRecord(@Header("Authorization") token: String, @Path("recordId") recordId: Long, @Body body: DispatchProcessReviewReq): AjaxResp<DispatchOrder>
    @POST("/app/manage/dispatch-orders/{id}/approve")
    suspend fun approve(@Header("Authorization") token: String, @Path("id") id: Long, @Body body: DispatchActionReq): AjaxResp<DispatchOrder>
    @POST("/app/manage/dispatch-orders/{id}/reject")
    suspend fun reject(@Header("Authorization") token: String, @Path("id") id: Long, @Body body: DispatchActionReq): AjaxResp<DispatchOrder>
    @POST("/app/manage/dispatch-orders/{id}/cancel")
    suspend fun cancel(@Header("Authorization") token: String, @Path("id") id: Long, @Body body: DispatchActionReq): AjaxResp<DispatchOrder>
    @POST("/app/manage/dispatch-orders/{id}/reassign-leader")
    suspend fun reassign(@Header("Authorization") token: String, @Path("id") id: Long, @Body body: DispatchReassignReq): AjaxResp<DispatchOrder>
}
