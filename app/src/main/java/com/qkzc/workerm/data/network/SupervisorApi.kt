package com.qkzc.workerm.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface SupervisorApi {

    @POST("/app/login")
    suspend fun login(@Body body: LoginReq): AjaxTokenResp

    @GET("/app/profile")
    suspend fun profile(@Header("Authorization") token: String): AjaxProfileResp

    @GET("/app/manage/projects")
    suspend fun manageProjects(
        @Header("Authorization") token: String,
    ): AjaxResp<List<ManagerProjectVo>>

    @GET("/app/manage/projects/{projectId}")
    suspend fun manageProjectDetail(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
    ): AjaxResp<ManagerProjectVo>

    @GET("/app/manage/projects/{projectId}/team-leaders")
    suspend fun manageProjectTeamLeaders(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
    ): AjaxResp<List<ManagerTeamLeaderVo>>

    @GET("/app/manage/home/overview")
    suspend fun manageHomeOverview(
        @Header("Authorization") token: String,
        @Query("projectId") projectId: Long? = null,
    ): AjaxResp<ManagerHomeOverviewVo>

    @GET("/app/manage/workers")
    suspend fun manageWorkers(
        @Header("Authorization") token: String,
        @Query("projectId") projectId: Long? = null,
        @Query("status") status: String? = null,
        @Query("keyword") keyword: String? = null,
    ): AjaxResp<List<ManagerWorkerVo>>

    @GET("/app/manage/workers/{workerId}")
    suspend fun manageWorkerDetail(
        @Header("Authorization") token: String,
        @Path("workerId") workerId: Long,
        @Query("projectId") projectId: Long,
    ): AjaxResp<ManagerWorkerVo>

    @POST("/app/manage/workers/scan-ticket")
    suspend fun manageWorkerScanTicket(
        @Header("Authorization") token: String,
        @Body body: ManagerWorkerScanReq,
    ): AjaxResp<ManagerWorkerVo>

    @POST("/app/manage/request/advance/list")
    suspend fun advanceList(
        @Header("Authorization") token: String,
        @Body body: AuditListReq,
    ): AjaxResp<List<AdvanceRequestVo>>

    @POST("/app/manage/request/advance/detail")
    suspend fun advanceDetail(
        @Header("Authorization") token: String,
        @Body body: AdvanceDetailReq,
    ): AjaxResp<AdvanceRequestVo>

    @POST("/app/manage/request/advance/audit")
    suspend fun auditAdvance(
        @Header("Authorization") token: String,
        @Body body: AdvanceAuditReq,
    ): AjaxResp<AdvanceRequestVo>

    @POST("/app/manage/request/material/list")
    suspend fun materialList(
        @Header("Authorization") token: String,
        @Body body: MaterialListReq,
    ): AjaxResp<List<MaterialRequestVo>>

    @POST("/app/manage/request/material/detail")
    suspend fun materialDetail(
        @Header("Authorization") token: String,
        @Body body: MaterialDetailReq,
    ): AjaxResp<MaterialRequestVo>

    @POST("/app/manage/request/material/audit")
    suspend fun auditMaterial(
        @Header("Authorization") token: String,
        @Body body: MaterialAuditReq,
    ): AjaxResp<MaterialRequestVo>

    @POST("/app/manage/exception/list")
    suspend fun exceptionList(
        @Header("Authorization") token: String,
        @Body body: AuditListReq,
    ): AjaxResp<List<AttendanceExceptionVo>>

    @POST("/app/manage/exception/detail")
    suspend fun exceptionDetail(
        @Header("Authorization") token: String,
        @Body body: ExceptionDetailReq,
    ): AjaxResp<AttendanceExceptionVo>

    @POST("/app/manage/exception/audit")
    suspend fun auditException(
        @Header("Authorization") token: String,
        @Body body: ExceptionAuditReq,
    ): AjaxResp<AttendanceExceptionVo>

    @POST("/app/manage/ai-warning/list")
    suspend fun manageAiWarningList(
        @Header("Authorization") token: String,
        @Body body: AiWarningListReq,
    ): AjaxResp<AiWarningPageVo>

    @POST("/app/manage/ai-warning/detail")
    suspend fun manageAiWarningDetail(
        @Header("Authorization") token: String,
        @Body body: AiWarningDetailReq,
    ): AjaxResp<AiWarningVo>

    @POST("/app/manage/ai-warning/read")
    suspend fun manageAiWarningRead(
        @Header("Authorization") token: String,
        @Body body: AiWarningReadReq,
    ): AjaxResp<Any>

    @POST("/app/manage/ai-warning/handle")
    suspend fun manageAiWarningHandle(
        @Header("Authorization") token: String,
        @Body body: AiWarningHandleReq,
    ): AjaxResp<Any>

    @POST("/app/manage/ai-warning/unread-count")
    suspend fun manageAiWarningUnreadCount(
        @Header("Authorization") token: String,
        @Body body: AiWarningListReq = AiWarningListReq(),
    ): AjaxResp<Int>
}
