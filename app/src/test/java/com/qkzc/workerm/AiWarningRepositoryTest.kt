package com.qkzc.workerm

import com.qkzc.workerm.data.aiwarning.AiWarningFilter
import com.qkzc.workerm.data.aiwarning.AiWarningRepository
import com.qkzc.workerm.data.aiwarning.model.AiWarningHandleStatus
import com.qkzc.workerm.data.network.AiWarningDetailReq
import com.qkzc.workerm.data.network.AiWarningHandleReq
import com.qkzc.workerm.data.network.AiWarningListReq
import com.qkzc.workerm.data.network.AiWarningPageVo
import com.qkzc.workerm.data.network.AiWarningReadReq
import com.qkzc.workerm.data.network.AiWarningVo
import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.SupervisorApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AiWarningRepositoryTest {

    @Test
    fun loadProjectManagerWarningsUsesManageEndpointsAndMapsRows() = runBlocking {
        val api = FakeSupervisorApi()
        val repository = AiWarningRepository(api)

        val page = repository.loadWarnings(
            token = "abc",
            filter = AiWarningFilter(
                riskLevel = "HIGH",
                handleStatus = AiWarningHandleStatus.PENDING,
                readFlag = 0,
                pageNum = 2,
                pageSize = 10,
            ),
        )

        assertEquals("Bearer abc", api.listToken)
        assertEquals(
            AiWarningListReq(
                readFlag = 0,
                handleStatus = "PENDING",
                riskLevel = "HIGH",
                pageNum = 2,
                pageSize = 10,
            ),
            api.listBody,
        )
        assertEquals("Bearer abc", api.unreadToken)
        assertEquals(5, page.unreadCount)
        assertEquals(12L, page.total)
        assertEquals(1, page.rows.size)
        assertEquals(7002L, page.rows.first().warningId)
        assertEquals("PROJECT_MANAGER", page.rows.first().receiverRole)
        assertEquals("严重", page.rows.first().riskLevelText)
    }

    @Test
    fun handleWarningSendsHandledStatus() = runBlocking {
        val api = FakeSupervisorApi()
        val repository = AiWarningRepository(api)

        repository.handle("abc", warningId = 7002L, handleStatus = AiWarningHandleStatus.HANDLED)

        assertEquals("Bearer abc", api.handleToken)
        assertEquals(
            AiWarningHandleReq(warningId = 7002L, handleStatus = "HANDLED"),
            api.handleBody,
        )
    }

    private class FakeSupervisorApi : SupervisorApi by EmptySupervisorApi() {
        var listToken: String? = null
        var listBody: AiWarningListReq? = null
        var unreadToken: String? = null
        var handleToken: String? = null
        var handleBody: AiWarningHandleReq? = null

        override suspend fun manageAiWarningList(
            token: String,
            body: AiWarningListReq,
        ): AjaxResp<AiWarningPageVo> {
            listToken = token
            listBody = body
            return AjaxResp(
                code = 200,
                msg = "ok",
                data = AiWarningPageVo(
                    total = 12,
                    pageNum = 2,
                    pageSize = 10,
                    pages = 2,
                    hasMore = true,
                    rows = listOf(
                        AiWarningVo(
                            warningId = 7002,
                            recordId = 9001,
                            receiverRole = "PROJECT_MANAGER",
                            workerUserId = 101,
                            workerName = "工人A",
                            projectId = 301,
                            projectName = "星河湾项目",
                            leaderId = 201,
                            leaderName = "班组长A",
                            sceneName = "3号楼施工区",
                            inspectionPoint = "2层临边",
                            workType = "钢筋工",
                            photoUrl = "https://oss.example.com/ai/inspection/a.jpg",
                            riskLevel = "HIGH",
                            riskScore = 88,
                            title = "AI巡查预警",
                            content = "发现未佩戴安全帽",
                            summary = "发现未佩戴安全帽",
                            readFlag = false,
                            handleStatus = "PENDING",
                            createTime = "2026-06-01 10:00:09",
                            readTime = null,
                            handleTime = null,
                        ),
                    ),
                ),
            )
        }

        override suspend fun manageAiWarningDetail(
            token: String,
            body: AiWarningDetailReq,
        ): AjaxResp<AiWarningVo> = error("unused")

        override suspend fun manageAiWarningRead(
            token: String,
            body: AiWarningReadReq,
        ): AjaxResp<Any> = AjaxResp(200, "ok", null)

        override suspend fun manageAiWarningHandle(
            token: String,
            body: AiWarningHandleReq,
        ): AjaxResp<Any> {
            handleToken = token
            handleBody = body
            return AjaxResp(200, "ok", null)
        }

        override suspend fun manageAiWarningUnreadCount(
            token: String,
            body: AiWarningListReq,
        ): AjaxResp<Int> {
            unreadToken = token
            return AjaxResp(200, "ok", 5)
        }
    }
}
