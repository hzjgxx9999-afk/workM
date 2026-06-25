package com.qkzc.workerm.data.material

import com.qkzc.workerm.data.network.ApiClient
import com.qkzc.workerm.data.network.MaterialInventoryChangeReq
import com.qkzc.workerm.data.network.MaterialInventoryItemVo
import com.qkzc.workerm.data.network.MaterialInventoryPageReq
import com.qkzc.workerm.data.network.MaterialStockRecordVo
import com.qkzc.workerm.data.network.MaterialStockWarningPageReq
import com.qkzc.workerm.data.network.MaterialStockWarningVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.network.bearerToken
import com.qkzc.workerm.data.network.requireSuccess

class MaterialRepository(
    private val api: SupervisorApi = ApiClient.supervisorApi,
) {
    suspend fun overview(token: String, projectId: Long): MaterialOverview {
        val response = api.materialInventoryOverview(bearerToken(token), projectId)
        requireSuccess(response.code, response.msg)
        val data = response.data
        return MaterialOverview(
            materialCount = data?.materialCount ?: 0,
            stockTotal = data?.stockTotal ?: 0.0,
            lowStockCount = data?.lowStockCount ?: 0,
            categories = data?.categories.orEmpty().map {
                MaterialCategory(
                    category = it.category.orEmpty(),
                    quantity = it.quantity ?: 0.0,
                    unit = it.unit.orEmpty(),
                )
            },
        )
    }

    suspend fun list(
        token: String,
        projectId: Long,
        keyword: String? = null,
        category: String? = null,
        pageSize: Int = 50,
    ): List<MaterialInventoryItem> {
        val response = api.materialInventoryPage(
            bearerToken(token),
            projectId,
            MaterialInventoryPageReq(
                pageNum = 1,
                pageSize = pageSize,
                category = category,
                itemName = keyword?.takeIf { it.isNotBlank() },
            ),
        )
        requireSuccess(response.code, response.msg)
        return response.data?.rows.orEmpty().map { it.toDomain() }
    }

    suspend fun detail(token: String, projectId: Long, materialId: Long): MaterialInventoryItem {
        val response = api.materialInventoryDetail(bearerToken(token), projectId, materialId)
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain() ?: MaterialInventoryItem(id = materialId, projectId = projectId)
    }

    suspend fun records(token: String, projectId: Long, materialId: Long? = null): List<MaterialStockRecord> {
        val response = api.materialInventoryRecords(bearerToken(token), projectId, materialId = materialId)
        requireSuccess(response.code, response.msg)
        return response.data.orEmpty().map { it.toDomain() }
    }

    suspend fun report(token: String, projectId: Long): MaterialReport {
        val response = api.materialInventoryReport(bearerToken(token), projectId)
        requireSuccess(response.code, response.msg)
        val data = response.data
        return MaterialReport(
            materialCount = data?.materialCount ?: 0,
            stockTotal = data?.stockTotal ?: 0.0,
            lowStockCount = data?.lowStockCount ?: 0,
            categories = data?.categories.orEmpty().map {
                MaterialCategory(it.category.orEmpty(), it.quantity ?: 0.0, it.unit.orEmpty())
            },
            records = data?.records.orEmpty().map { it.toDomain() },
        )
    }

    suspend fun inbound(token: String, projectId: Long, materialId: Long, warehouseId: Long, quantity: Double, remark: String?) {
        val response = api.materialInventoryInbound(
            bearerToken(token),
            projectId,
            MaterialInventoryChangeReq(materialId = materialId, warehouseId = warehouseId, quantity = quantity, remark = remark),
        )
        requireSuccess(response.code, response.msg)
    }

    suspend fun outbound(token: String, projectId: Long, materialId: Long, warehouseId: Long, quantity: Double, remark: String?) {
        val response = api.materialInventoryOutbound(
            bearerToken(token),
            projectId,
            MaterialInventoryChangeReq(materialId = materialId, warehouseId = warehouseId, quantity = quantity, remark = remark),
        )
        requireSuccess(response.code, response.msg)
    }

    suspend fun warningSummary(token: String, projectId: Long): MaterialWarningSummary {
        val response = api.materialStockWarningSummary(bearerToken(token), projectId)
        requireSuccess(response.code, response.msg)
        val data = response.data
        return MaterialWarningSummary(
            openCount = data?.openCount ?: 0,
            criticalCount = data?.criticalCount ?: 0,
            unreadCount = data?.unreadCount ?: 0,
        )
    }

    suspend fun warningPage(
        token: String,
        projectId: Long,
        status: String? = STATUS_OPEN,
        warningLevel: String? = null,
        pageSize: Int = 50,
    ): List<MaterialStockWarning> {
        val response = api.materialStockWarningPage(
            bearerToken(token),
            projectId,
            MaterialStockWarningPageReq(
                pageNum = 1,
                pageSize = pageSize,
                status = status,
                warningLevel = warningLevel,
            ),
        )
        requireSuccess(response.code, response.msg)
        return response.data?.rows.orEmpty().map { it.toDomain() }
    }

    suspend fun ackWarning(token: String, projectId: Long, warningId: Long) {
        val response = api.ackMaterialStockWarning(bearerToken(token), projectId, warningId)
        requireSuccess(response.code, response.msg)
    }

    private fun MaterialInventoryItemVo.toDomain() = MaterialInventoryItem(
        id = id ?: 0L,
        projectId = projectId ?: 0L,
        category = category.orEmpty(),
        itemName = itemName.orEmpty(),
        specModel = specModel.orEmpty(),
        unit = unit.orEmpty(),
        materialCode = materialCode.orEmpty(),
        safeStock = safeStock ?: 0.0,
        maxStock = maxStock ?: 0.0,
        status = status.orEmpty(),
        currentQty = currentQty ?: 0.0,
        lockedQty = lockedQty ?: 0.0,
        warehouseId = warehouseId ?: 0L,
        warehouseName = warehouseName.orEmpty(),
        locationText = locationText.orEmpty(),
        managerName = managerName.orEmpty(),
        contactPhone = contactPhone.orEmpty(),
        lowStock = lowStock == true,
    )

    private fun MaterialStockRecordVo.toDomain() = MaterialStockRecord(
        id = id ?: 0L,
        materialId = materialId ?: 0L,
        warehouseId = warehouseId ?: 0L,
        bizType = bizType.orEmpty(),
        bizNo = bizNo.orEmpty(),
        changeQty = changeQty ?: 0.0,
        beforeQty = beforeQty ?: 0.0,
        afterQty = afterQty ?: 0.0,
        operatorName = operatorName.orEmpty(),
        itemName = itemName.orEmpty(),
        unit = unit.orEmpty(),
        warehouseName = warehouseName.orEmpty(),
        remark = remark.orEmpty(),
        createTime = createTime.orEmpty(),
    )

    private fun MaterialStockWarningVo.toDomain() = MaterialStockWarning(
        warningId = warningId ?: 0L,
        projectId = projectId ?: 0L,
        materialId = materialId ?: 0L,
        itemName = itemName.orEmpty(),
        unit = unit.orEmpty(),
        materialCode = materialCode.orEmpty(),
        category = category.orEmpty(),
        warningType = warningType.orEmpty(),
        warningLevel = warningLevel.orEmpty(),
        status = status.orEmpty(),
        currentQty = currentQty ?: 0.0,
        lockedQty = lockedQty ?: 0.0,
        availableQty = availableQty ?: 0.0,
        safeStock = safeStock ?: 0.0,
        shortageQty = shortageQty ?: 0.0,
        firstTriggerTime = firstTriggerTime.orEmpty(),
        lastTriggerTime = lastTriggerTime.orEmpty(),
        ackTime = ackTime.orEmpty(),
        resolvedTime = resolvedTime.orEmpty(),
    )

    companion object {
        const val STATUS_OPEN = "OPEN"
    }
}

data class MaterialOverview(
    val materialCount: Int = 0,
    val stockTotal: Double = 0.0,
    val lowStockCount: Int = 0,
    val categories: List<MaterialCategory> = emptyList(),
)

data class MaterialCategory(
    val category: String,
    val quantity: Double,
    val unit: String,
)

data class MaterialInventoryItem(
    val id: Long = 0L,
    val projectId: Long = 0L,
    val category: String = "",
    val itemName: String = "",
    val specModel: String = "",
    val unit: String = "",
    val materialCode: String = "",
    val safeStock: Double = 0.0,
    val maxStock: Double = 0.0,
    val status: String = "",
    val currentQty: Double = 0.0,
    val lockedQty: Double = 0.0,
    val warehouseId: Long = 0L,
    val warehouseName: String = "",
    val locationText: String = "",
    val managerName: String = "",
    val contactPhone: String = "",
    val lowStock: Boolean = false,
)

data class MaterialStockRecord(
    val id: Long = 0L,
    val materialId: Long = 0L,
    val warehouseId: Long = 0L,
    val bizType: String = "",
    val bizNo: String = "",
    val changeQty: Double = 0.0,
    val beforeQty: Double = 0.0,
    val afterQty: Double = 0.0,
    val operatorName: String = "",
    val itemName: String = "",
    val unit: String = "",
    val warehouseName: String = "",
    val remark: String = "",
    val createTime: String = "",
)

data class MaterialReport(
    val materialCount: Int = 0,
    val stockTotal: Double = 0.0,
    val lowStockCount: Int = 0,
    val categories: List<MaterialCategory> = emptyList(),
    val records: List<MaterialStockRecord> = emptyList(),
)

data class MaterialWarningSummary(
    val openCount: Int = 0,
    val criticalCount: Int = 0,
    val unreadCount: Int = 0,
)

data class MaterialStockWarning(
    val warningId: Long = 0L,
    val projectId: Long = 0L,
    val materialId: Long = 0L,
    val itemName: String = "",
    val unit: String = "",
    val materialCode: String = "",
    val category: String = "",
    val warningType: String = "",
    val warningLevel: String = "",
    val status: String = "",
    val currentQty: Double = 0.0,
    val lockedQty: Double = 0.0,
    val availableQty: Double = 0.0,
    val safeStock: Double = 0.0,
    val shortageQty: Double = 0.0,
    val firstTriggerTime: String = "",
    val lastTriggerTime: String = "",
    val ackTime: String = "",
    val resolvedTime: String = "",
)
