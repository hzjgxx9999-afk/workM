package com.qkzc.workerm.ui.dispatch

data class DispatchRequirementText(
    val content: String,
    val constructionRequirement: String,
    val safetyNotice: String,
)

object DispatchRequirementTemplate {
    fun build(
        dispatchType: String,
        title: String,
        location: String,
    ): DispatchRequirementText {
        val normalizedType = dispatchType.uppercase()
        val cleanTitle = title.trim()
        val cleanLocation = location.trim()
        val target = cleanTitle.ifBlank { typeLabel(normalizedType) }
        val locationText = cleanLocation.takeIf { it.isNotBlank() }?.let { "，施工位置：$it" }.orEmpty()

        return when (normalizedType) {
            "REPAIR" -> DispatchRequirementText(
                content = "完成${target}${locationText}，排查故障原因，修复后进行功能确认并反馈处理结果。",
                constructionRequirement = "维修前确认故障范围和影响区域，必要时先断电、隔离或设置警戒；维修完成后清理现场并恢复设备状态。",
                safetyNotice = "涉及电气、动火、高处或受限空间作业时必须先办理相应审批，断电验电，佩戴防护用品，禁止带病冒险作业。",
            )
            "INSTALL" -> DispatchRequirementText(
                content = "完成${target}${locationText}，按图纸和现场交底要求完成安装、固定、调试和自检。",
                constructionRequirement = "安装前核对材料规格、安装位置和作业面条件；安装过程控制垂直度、牢固度和成品保护，完工后提交现场照片。",
                safetyNotice = "搬运和安装时做好防砸、防滑、防坠落措施；使用电动工具前检查电源线和防护罩，严禁无防护交叉作业。",
            )
            "CLEAN" -> DispatchRequirementText(
                content = "完成${target}${locationText}，清理作业面、通道和堆放区域，保持现场整洁可通行。",
                constructionRequirement = "按区域分段清理，建筑垃圾分类堆放并及时转运；不得遮挡消防通道、配电箱和安全标识。",
                safetyNotice = "清理前确认周边临边洞口和高空坠物风险，佩戴安全帽、手套和防尘用品，湿滑区域设置提示。",
            )
            "RECTIFICATION" -> DispatchRequirementText(
                content = "完成${target}${locationText}，按整改要求闭环处理问题，并提交整改前后对比照片。",
                constructionRequirement = "先核对整改通知或现场问题点，明确整改标准、责任边界和完成时限；整改完成后自检并反馈验收依据。",
                safetyNotice = "整改期间设置警戒和临时防护，涉及拆改、临电、高处作业时按专项安全要求执行，严禁擅自扩大作业范围。",
            )
            "INSPECTION" -> DispatchRequirementText(
                content = "按派工类型完成${target}${locationText}任务，记录问题并反馈处理结果。",
                constructionRequirement = "按检查清单逐项核对现场质量、安全、材料和进度情况，形成检查记录；发现问题拍照定位并说明责任班组。",
                safetyNotice = "巡检进入施工区域必须佩戴防护用品，注意车辆、吊装、临边洞口和交叉作业风险，不得单独进入危险区域。",
            )
            else -> DispatchRequirementText(
                content = "完成${target}${locationText}，按现场交底要求执行，完工后反馈处理结果。",
                constructionRequirement = "开工前确认作业范围、质量标准和完成时限；施工中保持现场整洁，完工后提交必要照片和说明。",
                safetyNotice = "作业人员必须佩戴安全防护用品，遵守现场安全管理要求，发现隐患立即停止作业并上报。",
            )
        }
    }

    private fun typeLabel(dispatchType: String): String = when (dispatchType) {
        "REPAIR" -> "现场维修任务"
        "INSTALL" -> "现场安装任务"
        "CLEAN" -> "现场清理任务"
        "RECTIFICATION" -> "现场整改任务"
        "INSPECTION" -> "现场巡检"
        else -> "现场派工任务"
    }
}
