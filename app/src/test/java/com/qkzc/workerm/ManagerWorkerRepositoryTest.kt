package com.qkzc.workerm

import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ManagerWorkerScanReq
import com.qkzc.workerm.data.network.ManagerWorkerVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.worker.ManagerWorkerRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ManagerWorkerRepositoryTest {

    @Test
    fun detailSendsProjectAndWorkerScopeAndMapsBindFields() = runBlocking {
        val api = FakeWorkerApi()
        val repository = ManagerWorkerRepository(api)

        val worker = repository.detail("token", projectId = 10, workerUserId = 501)

        assertEquals("Bearer token", api.detailToken)
        assertEquals(10L, api.detailProjectId)
        assertEquals(501L, api.detailWorkerId)
        assertEquals("钢筋班组", worker.teamName)
        assertEquals("ACTIVE", worker.bindStatus)
        assertEquals("COMPLETED", worker.entryStatus)
    }

    @Test
    fun scanTicketSendsProjectAndWorkerScope() = runBlocking {
        val api = FakeWorkerApi()
        val repository = ManagerWorkerRepository(api)

        val worker = repository.scanTicket("token", projectId = 10, workerUserId = 501)

        assertEquals("Bearer token", api.scanToken)
        assertEquals(ManagerWorkerScanReq(projectId = 10, workerUserId = 501), api.scanBody)
        assertEquals(501, worker.workerUserId)
        assertEquals("张三", worker.realName)
        assertEquals("钢筋班组", worker.leaderName)
    }

    @Test
    fun scanQrTicketSendsProjectAndTicketOnly() = runBlocking {
        val api = FakeWorkerApi()
        val repository = ManagerWorkerRepository(api)

        val worker = repository.scanQrTicket("token", projectId = 10, ticket = "ticket-1")

        assertEquals("Bearer token", api.scanToken)
        assertEquals(ManagerWorkerScanReq(projectId = 10, ticket = "ticket-1"), api.scanBody)
        assertEquals(501, worker.workerUserId)
    }

    private class FakeWorkerApi : SupervisorApi by EmptySupervisorApi() {
        var detailToken: String? = null
        var detailProjectId: Long? = null
        var detailWorkerId: Long? = null
        var scanToken: String? = null
        var scanBody: ManagerWorkerScanReq? = null

        override suspend fun manageWorkerDetail(
            token: String,
            workerId: Long,
            projectId: Long,
        ): AjaxResp<ManagerWorkerVo> {
            detailToken = token
            detailWorkerId = workerId
            detailProjectId = projectId
            return AjaxResp(
                code = 200,
                msg = "ok",
                data = ManagerWorkerVo(
                    workerUserId = workerId,
                    projectId = projectId,
                    realName = "张三",
                    workTypeName = "钢筋工",
                    leaderName = "李师傅",
                    teamName = "钢筋班组",
                    bindStatus = "ACTIVE",
                    entryStatus = "COMPLETED",
                ),
            )
        }

        override suspend fun manageWorkerScanTicket(
            token: String,
            body: ManagerWorkerScanReq,
        ): AjaxResp<ManagerWorkerVo> {
            scanToken = token
            scanBody = body
            return AjaxResp(
                code = 200,
                msg = "ok",
                data = ManagerWorkerVo(
                    workerUserId = 501,
                    realName = "张三",
                    workTypeName = "钢筋工",
                    leaderName = "钢筋班组",
                    projectName = "一号项目",
                ),
            )
        }
    }
}
