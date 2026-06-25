package com.qkzc.workerm

import com.qkzc.workerm.data.material.MaterialRepository
import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.MaterialCategorySummaryVo
import com.qkzc.workerm.data.network.MaterialInventoryItemVo
import com.qkzc.workerm.data.network.MaterialInventoryPageReq
import com.qkzc.workerm.data.network.MaterialOverviewVo
import com.qkzc.workerm.data.network.MaterialStockWarningPageReq
import com.qkzc.workerm.data.network.MaterialStockWarningSummaryVo
import com.qkzc.workerm.data.network.MaterialStockWarningVo
import com.qkzc.workerm.data.network.PageResp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialRepositoryTest {
    @Test
    fun parsesOverviewAndInventoryPage() = runBlocking {
        val repository = MaterialRepository(FakeMaterialApi())

        val overview = repository.overview("token", 100L)
        val rows = repository.list("token", 100L)

        assertEquals(2, overview.materialCount)
        assertEquals(1, overview.lowStockCount)
        assertEquals("钢筋", overview.categories.first().category)
        assertEquals(10L, rows.first().id)
        assertEquals(3L, rows.first().warehouseId)
        assertTrue(rows.first().lowStock)
    }

    @Test
    fun parsesMaterialStockWarningSummaryAndPage() = runBlocking {
        val api = FakeMaterialApi()
        val repository = MaterialRepository(api)

        val summary = repository.warningSummary("token", 100L)
        val warnings = repository.warningPage("token", 100L)
        repository.ackWarning("token", 100L, 9001L)

        assertEquals("Bearer token", api.warningSummaryToken)
        assertEquals(2, summary.openCount)
        assertEquals(1, summary.criticalCount)
        assertEquals(2, summary.unreadCount)
        assertEquals("Bearer token", api.warningPageToken)
        assertEquals("OPEN", api.warningPageBody?.status)
        assertEquals(9001L, warnings.first().warningId)
        assertEquals("水泥", warnings.first().itemName)
        assertEquals("WARNING", warnings.first().warningLevel)
        assertEquals(12.0, warnings.first().availableQty, 0.001)
        assertEquals("Bearer token", api.ackToken)
        assertEquals(9001L, api.ackWarningId)
    }

    private class FakeMaterialApi : EmptySupervisorApi() {
        var warningSummaryToken: String? = null
        var warningPageToken: String? = null
        var warningPageBody: MaterialStockWarningPageReq? = null
        var ackToken: String? = null
        var ackWarningId: Long? = null

        override suspend fun materialInventoryOverview(token: String, projectId: Long) =
            AjaxResp(
                code = 200,
                msg = "ok",
                data = MaterialOverviewVo(
                    materialCount = 2,
                    stockTotal = 120.0,
                    lowStockCount = 1,
                    categories = listOf(MaterialCategorySummaryVo("钢筋", 80.0, "吨")),
                ),
            )

        override suspend fun materialInventoryPage(
            token: String,
            projectId: Long,
            body: MaterialInventoryPageReq,
        ) = AjaxResp(
            code = 200,
            msg = "ok",
            data = PageResp(
                total = 1,
                rows = listOf(
                    MaterialInventoryItemVo(
                        id = 10L,
                        projectId = projectId,
                        category = "钢筋",
                        itemName = "钢筋 HRB400E",
                        unit = "吨",
                        currentQty = 80.0,
                        safeStock = 100.0,
                        warehouseId = 3L,
                        warehouseName = "1#材料堆场",
                        lowStock = true,
                    ),
                ),
            ),
        )

        override suspend fun materialStockWarningSummary(
            token: String,
            projectId: Long,
        ): AjaxResp<MaterialStockWarningSummaryVo> {
            warningSummaryToken = token
            return AjaxResp(
                code = 200,
                msg = "ok",
                data = MaterialStockWarningSummaryVo(openCount = 2, criticalCount = 1, unreadCount = 2),
            )
        }

        override suspend fun materialStockWarningPage(
            token: String,
            projectId: Long,
            body: MaterialStockWarningPageReq,
        ): AjaxResp<PageResp<MaterialStockWarningVo>> {
            warningPageToken = token
            warningPageBody = body
            return AjaxResp(
                code = 200,
                msg = "ok",
                data = PageResp(
                    total = 1,
                    rows = listOf(
                        MaterialStockWarningVo(
                            warningId = 9001L,
                            projectId = projectId,
                            materialId = 10L,
                            itemName = "水泥",
                            unit = "袋",
                            warningLevel = "WARNING",
                            status = "OPEN",
                            currentQty = 12.0,
                            lockedQty = 0.0,
                            availableQty = 12.0,
                            safeStock = 20.0,
                            shortageQty = 8.0,
                            firstTriggerTime = "2026-06-24 10:00:00",
                            lastTriggerTime = "2026-06-24 10:30:00",
                        ),
                    ),
                ),
            )
        }

        override suspend fun ackMaterialStockWarning(
            token: String,
            projectId: Long,
            warningId: Long,
        ): AjaxResp<Any> {
            ackToken = token
            ackWarningId = warningId
            return AjaxResp(200, "ok", null)
        }
    }
}
