package com.qkzc.workerm.data.network

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Path
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.PUT
import okhttp3.MultipartBody
import retrofit2.http.Streaming

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

    @GET("/app/manage/projects/{projectId}/geofence")
    suspend fun manageProjectGeofence(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
    ): AjaxResp<com.qkzc.workerm.data.location.ProjectGeofence>

    @PUT("/app/manage/projects/{projectId}/geofence")
    suspend fun saveManageProjectGeofence(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Body body: com.qkzc.workerm.data.location.ProjectGeofenceSaveRequest,
    ): AjaxResp<com.qkzc.workerm.data.location.ProjectGeofence>

    @DELETE("/app/manage/projects/{projectId}/geofence")
    suspend fun disableManageProjectGeofence(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
    ): AjaxResp<Any>

    @GET("/app/manage/projects/{projectId}/drawing-docs")
    suspend fun manageProjectDrawingDocs(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Query("limit") limit: Int? = null,
    ): AjaxResp<List<ManagerProjectFileVo>>

    @Streaming
    @GET("/app/manage/projects/{projectId}/cover/preview")
    suspend fun manageProjectCoverPreview(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
    ): ResponseBody

    @GET("/app/manage/projects/{projectId}/team-leaders")
    suspend fun manageProjectTeamLeaders(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
    ): AjaxResp<List<ManagerTeamLeaderVo>>

    @GET("/app/manage/projects/{projectId}/teams")
    suspend fun manageProjectTeams(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
    ): AjaxResp<List<ManagerProjectTeamVo>>

    @GET("/app/manage/projects/{projectId}/worker-stats")
    suspend fun manageProjectWorkerStats(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
    ): AjaxResp<ManagerWorkerStatsVo>

    @GET("/app/manage/projects/{projectId}/materials/overview")
    suspend fun materialInventoryOverview(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
    ): AjaxResp<MaterialOverviewVo>

    @POST("/app/manage/projects/{projectId}/materials/page")
    suspend fun materialInventoryPage(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Body body: MaterialInventoryPageReq,
    ): AjaxResp<PageResp<MaterialInventoryItemVo>>

    @GET("/app/manage/projects/{projectId}/materials/{materialId}")
    suspend fun materialInventoryDetail(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Path("materialId") materialId: Long,
    ): AjaxResp<MaterialInventoryItemVo>

    @GET("/app/manage/projects/{projectId}/materials/records")
    suspend fun materialInventoryRecords(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Query("materialId") materialId: Long? = null,
        @Query("warehouseId") warehouseId: Long? = null,
        @Query("bizType") bizType: String? = null,
    ): AjaxResp<List<MaterialStockRecordVo>>

    @GET("/app/manage/projects/{projectId}/materials/report")
    suspend fun materialInventoryReport(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
    ): AjaxResp<MaterialReportVo>

    @POST("/app/manage/projects/{projectId}/materials/inbound")
    suspend fun materialInventoryInbound(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Body body: MaterialInventoryChangeReq,
    ): AjaxResp<MaterialStockVo>

    @POST("/app/manage/projects/{projectId}/materials/outbound")
    suspend fun materialInventoryOutbound(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Body body: MaterialInventoryChangeReq,
    ): AjaxResp<MaterialStockVo>

    @GET("/app/manage/projects/{projectId}/materials/warnings/summary")
    suspend fun materialStockWarningSummary(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
    ): AjaxResp<MaterialStockWarningSummaryVo>

    @POST("/app/manage/projects/{projectId}/materials/warnings/page")
    suspend fun materialStockWarningPage(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Body body: MaterialStockWarningPageReq,
    ): AjaxResp<PageResp<MaterialStockWarningVo>>

    @POST("/app/manage/projects/{projectId}/materials/warnings/{warningId}/ack")
    suspend fun ackMaterialStockWarning(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Path("warningId") warningId: Long,
    ): AjaxResp<Any>

    @GET("/app/manage/projects/{projectId}/leader-options")
    suspend fun manageProjectLeaderOptions(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Query("keyword") keyword: String? = null,
    ): AjaxResp<List<ManagerLeaderOptionVo>>

    @GET("/app/manage/projects/{projectId}/work-types")
    suspend fun manageProjectWorkTypes(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Query("keyword") keyword: String? = null,
    ): AjaxResp<List<ManagerWorkTypeOptionVo>>

    @POST("/app/manage/projects/{projectId}/teams")
    suspend fun createManageProjectTeam(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Body body: ManagerProjectTeamSaveReq,
    ): AjaxResp<ManagerProjectTeamVo>

    @PUT("/app/manage/projects/{projectId}/teams/{teamId}")
    suspend fun updateManageProjectTeam(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Path("teamId") teamId: Long,
        @Body body: ManagerProjectTeamSaveReq,
    ): AjaxResp<ManagerProjectTeamVo>

    @PUT("/app/manage/projects/{projectId}/teams/{teamId}/status")
    suspend fun updateManageProjectTeamStatus(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Path("teamId") teamId: Long,
        @Body body: ManagerProjectTeamStatusReq,
    ): AjaxResp<Any>

    @GET("/app/manage/invite-code/list")
    suspend fun manageInviteCodeList(
        @Header("Authorization") token: String,
        @Query("projectId") projectId: Long? = null,
        @Query("status") status: String? = null,
        @Query("inviteCode") inviteCode: String? = null,
    ): AjaxResp<List<ManageInviteCodeVo>>

    @POST("/app/manage/invite-code")
    suspend fun createManageInviteCode(
        @Header("Authorization") token: String,
        @Body body: ManageInviteCodeCreateReq,
    ): AjaxResp<ManageInviteCodeVo>

    @PUT("/app/manage/invite-code/{id}/status")
    suspend fun updateManageInviteCodeStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body body: ManageInviteCodeStatusReq,
    ): AjaxResp<Any>

    @DELETE("/app/manage/invite-code/{id}")
    suspend fun deleteManageInviteCode(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
    ): AjaxResp<Any>

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

    @GET("/app/manage/workers/{workerId}/relations")
    suspend fun manageWorkerRelations(
        @Header("Authorization") token: String,
        @Path("workerId") workerId: Long,
        @Query("projectId") projectId: Long? = null,
    ): AjaxResp<List<WorkerBindRelationVo>>

    @POST("/app/manage/workers/scan-ticket")
    suspend fun manageWorkerScanTicket(
        @Header("Authorization") token: String,
        @Body body: ManagerWorkerScanReq,
    ): AjaxResp<ManagerWorkerVo>

    @POST("/app/qr/worker/verify")
    suspend fun verifyWorkerQr(
        @Header("Authorization") token: String,
        @Body body: WorkerQrVerifyReq,
    ): AjaxResp<WorkerQrVerifyVo>

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

    @POST("/app/manage/exit/list")
    suspend fun exitList(
        @Header("Authorization") token: String,
        @Body body: ExitListReq,
    ): AjaxResp<List<ExitRequestVo>>

    @POST("/app/manage/exit/detail")
    suspend fun exitDetail(
        @Header("Authorization") token: String,
        @Body body: ExitDetailReq,
    ): AjaxResp<ExitRequestVo>

    @POST("/app/manage/exit/audit")
    suspend fun auditExit(
        @Header("Authorization") token: String,
        @Body body: ExitAuditReq,
    ): AjaxResp<ExitRequestVo>

    @GET("/app/manage/bind-change-requests")
    suspend fun bindChangeRequests(
        @Header("Authorization") token: String,
        @Query("projectId") projectId: Long? = null,
        @Query("status") status: String? = null,
    ): AjaxResp<List<WorkerBindChangeRequestVo>>

    @POST("/app/manage/bind-change-requests/{id}/approve")
    suspend fun approveBindChangeRequest(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body body: BindChangeAuditReq,
    ): AjaxResp<WorkerBindChangeRequestVo>

    @POST("/app/manage/bind-change-requests/{id}/reject")
    suspend fun rejectBindChangeRequest(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body body: BindChangeAuditReq,
    ): AjaxResp<WorkerBindChangeRequestVo>

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

    @POST("/app/manage/ai-warning/summary")
    suspend fun manageAiWarningSummary(
        @Header("Authorization") token: String,
        @Body body: AiWarningListReq = AiWarningListReq(),
    ): AjaxResp<AiWarningSummaryVo>

    @GET("/app/manage/projects/{projectId}/construction-logs/overview")
    suspend fun constructionLogOverview(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
    ): AjaxResp<ConstructionLogOverviewVo>

    @POST("/app/manage/projects/{projectId}/construction-logs/page")
    suspend fun constructionLogPage(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Body body: ConstructionLogPageReq,
    ): AjaxResp<PageResp<ConstructionLogItemVo>>

    @GET("/app/manage/projects/{projectId}/construction-logs/{logId}")
    suspend fun constructionLogDetail(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Path("logId") logId: Long,
    ): AjaxResp<ConstructionLogDetailVo>

    @POST("/app/manage/projects/{projectId}/construction-logs")
    suspend fun createConstructionLog(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Body body: ConstructionLogSaveReq,
    ): AjaxResp<ConstructionLogDetailVo>

    @PUT("/app/manage/projects/{projectId}/construction-logs/{logId}")
    suspend fun updateConstructionLog(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Path("logId") logId: Long,
        @Body body: ConstructionLogSaveReq,
    ): AjaxResp<ConstructionLogDetailVo>

    @POST("/app/manage/projects/{projectId}/construction-logs/{logId}/delete")
    suspend fun deleteConstructionLog(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Path("logId") logId: Long,
        @Body body: VersionReq,
    ): AjaxResp<Any>

    @POST("/app/manage/projects/{projectId}/construction-logs/{logId}/submit")
    suspend fun submitConstructionLog(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Path("logId") logId: Long,
        @Body body: VersionReq,
    ): AjaxResp<Any>

    @POST("/app/manage/projects/{projectId}/construction-logs/{logId}/withdraw")
    suspend fun withdrawConstructionLog(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Path("logId") logId: Long,
        @Body body: VersionReq,
    ): AjaxResp<Any>

    @POST("/app/manage/projects/{projectId}/construction-logs/{logId}/approve")
    suspend fun approveConstructionLog(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Path("logId") logId: Long,
        @Body body: ConstructionLogAuditReq,
    ): AjaxResp<ConstructionLogDetailVo>

    @POST("/app/manage/projects/{projectId}/construction-logs/{logId}/reject")
    suspend fun rejectConstructionLog(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Path("logId") logId: Long,
        @Body body: ConstructionLogAuditReq,
    ): AjaxResp<ConstructionLogDetailVo>

    @Multipart
    @POST("/app/manage/projects/{projectId}/drawing-docs/upload")
    suspend fun uploadProjectDrawingDoc(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: Long,
        @Part file: MultipartBody.Part,
        @Part("category") category: okhttp3.RequestBody,
        @Part("remark") remark: okhttp3.RequestBody,
    ): AjaxResp<ManagerProjectFileVo>

    @Multipart
    @POST("/app/common/files/upload")
    suspend fun uploadCommonFile(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
    ): AjaxResp<FileUploadVo>
}
