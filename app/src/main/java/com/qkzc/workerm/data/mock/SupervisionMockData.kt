package com.qkzc.workerm.data.mock

import com.qkzc.workerm.ui.model.DashboardMetricUi
import com.qkzc.workerm.ui.model.FeatureCardUi
import com.qkzc.workerm.ui.model.InfoRowUi
import com.qkzc.workerm.ui.model.QuickActionUi

object SupervisionMockData {

    fun dashboardMetrics(): List<DashboardMetricUi> = listOf(
        DashboardMetricUi("今日出勤", "268", "到岗率 92%", "#E8F1FB"),
        DashboardMetricUi("未到岗", "21", "较昨日 -6", "#FFF2E2"),
        DashboardMetricUi("待整改隐患", "17", "超期 4 条", "#FCEBEC"),
        DashboardMetricUi("待审批", "12", "补卡 5 条", "#EAF7F1"),
        DashboardMetricUi("超时工单", "8", "本周新增 2", "#FFF2E2"),
        DashboardMetricUi("告警数", "5", "设备告警 3", "#FCEBEC"),
    )

    fun riskWarnings(): List<InfoRowUi> = listOf(
        InfoRowUi(
            title = "2 号塔吊离线超过 30 分钟",
            subtitle = "设备告警 · 机电班组 · 需要立即确认",
            tag = "高风险",
            trailing = "P1",
        ),
        InfoRowUi(
            title = "焊接作业区发现未佩戴防护面罩",
            subtitle = "现场巡检 · 今日 09:20 · 已生成隐患单",
            tag = "处理中",
            trailing = "2h",
        ),
        InfoRowUi(
            title = "3 名劳务工证件将在 7 天内到期",
            subtitle = "人员管理 · 华东片区 · 需要提前续证",
            tag = "提醒",
            trailing = "3人",
        ),
    )

    fun pendingTasks(): List<InfoRowUi> = listOf(
        InfoRowUi(
            title = "补卡审批待处理",
            subtitle = "木工班组提交 5 条补卡申请",
            tag = "审批",
            trailing = "5",
        ),
        InfoRowUi(
            title = "入场申请待审批",
            subtitle = "钢筋班组新增 2 名工人等待实名入场审核",
            tag = "入场",
            trailing = "2",
        ),
        InfoRowUi(
            title = "请假申请已超 2 小时未处理",
            subtitle = "项目 A 夜班请假流程滞留在项目经理节点",
            tag = "超时",
            trailing = "1",
        ),
    )

    fun quickActions(): List<QuickActionUi> = listOf(
        QuickActionUi("驾驶舱", android.R.drawable.ic_menu_mapmode, "#DCEBFF", "cockpit"),
        QuickActionUi("视频监控", android.R.drawable.ic_menu_slideshow, "#E6F6ED", "video"),
        QuickActionUi("扫码验证", android.R.drawable.ic_menu_camera, "#FFF0D8", "scan"),
        QuickActionUi("隐患", android.R.drawable.ic_dialog_alert, "#FCE4E4", "hazard"),
        QuickActionUi("工单", android.R.drawable.ic_menu_agenda, "#EAF0FF", "workorder"),
        QuickActionUi("审批", android.R.drawable.ic_menu_edit, "#E6F6ED", "approval"),
        QuickActionUi("消息", android.R.drawable.ic_dialog_email, "#FFF0D8", "message"),
        QuickActionUi("报表", android.R.drawable.ic_menu_sort_by_size, "#FCE4E4", "report"),
    )

    fun supervisionFeatures(): List<FeatureCardUi> = listOf(
        FeatureCardUi(
            title = "项目驾驶舱",
            description = "查看项目实时定位、在线人数、风险热区和项目态势总览。",
            phase = "一期主线",
        ),
        FeatureCardUi(
            title = "视频监控",
            description = "接入摄像头通道、重点区域轮巡和违规行为回看。",
            phase = "一期主线",
        ),
        FeatureCardUi(
            title = "巡检与隐患",
            description = "巡检计划、隐患上报、整改复查与归档闭环。",
            phase = "一期主线",
        ),
        FeatureCardUi(
            title = "工单监管",
            description = "工单分派、转派、处理记录、超时预警和关闭。",
            phase = "一期主线",
        ),
        FeatureCardUi(
            title = "审批中心",
            description = "待我审批、我已审批、流程轨迹与动作留痕。",
            phase = "一期主线",
        ),
        FeatureCardUi(
            title = "设备与报表",
            description = "设备告警、KPI 趋势、项目排行，二期增强。",
            phase = "二期增强",
        ),
    )

    fun todoItems(): List<InfoRowUi> = listOf(
        InfoRowUi("高空平台隐患复查", "需要在今日 18:00 前完成复查验收", "紧急", "今日"),
        InfoRowUi("审批流转超时", "外协班组入场申请在第 2 节点停留 6 小时", "审批", "6h"),
        InfoRowUi("塔吊离线排查", "设备告警仍未恢复，需联系维保人员", "设备", "P1"),
        InfoRowUi("临电工单退回", "施工区 B 配电箱工单被退回，请重新分派", "工单", "退回"),
    )

    fun messageItems(): List<InfoRowUi> = listOf(
        InfoRowUi("审批通知", "今日新增 5 条补卡审批，2 条入场审批", "未读", "7"),
        InfoRowUi("风险预警", "隐患、设备、证件类预警共 6 条", "重点", "6"),
        InfoRowUi("系统通知", "监管端一期环境已更新到测试服", "系统", "1"),
        InfoRowUi("项目消息", "华东片区项目组织树已同步最新结构", "同步", "完成"),
    )

    fun profileActions(): List<InfoRowUi> = listOf(
        InfoRowUi("当前项目切换", "切换组织、项目和标段上下文"),
        InfoRowUi("角色与权限", "查看当前角色、菜单权限和数据范围"),
        InfoRowUi("缓存清理", "清理图片、附件和离线列表缓存"),
        InfoRowUi("关于应用", "查看版本号、更新记录和技术支持"),
    )
}
