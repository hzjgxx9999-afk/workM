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

    private class FakeWorkerApi : SupervisorApi by EmptySupervisorApi() {
        var scanToken: String? = null
        var scanBody: ManagerWorkerScanReq? = null

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
