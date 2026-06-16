package com.qkzc.workerm

import com.qkzc.workerm.data.material.MaterialRepository
import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.MaterialCategorySummaryVo
import com.qkzc.workerm.data.network.MaterialInventoryItemVo
import com.qkzc.workerm.data.network.MaterialInventoryPageReq
import com.qkzc.workerm.data.network.MaterialOverviewVo
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

    private class FakeMaterialApi : EmptySupervisorApi() {
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
    }
}
