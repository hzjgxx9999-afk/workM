# 监管端 App 完整功能架构图与模块划分文档

## 1. 文档说明

本文档用于指导在现有工人端基础上，新建独立的管理监管端 App。目标是形成一套可落地、可评审、可实施的产品与技术方案，便于后续按阶段推进研发。

适用对象：
- 产品经理
- Android 开发
- 后端开发
- 测试
- 项目经理
- 技术负责人

---

## 2. 建设目标

在已有工人端之外，新建一个独立的监管端 App，面向：
- 企业管理员
- 项目经理
- 安全员
- 监管员
- 班组长
- 监理/外部监管角色

监管端核心目标不是简单展示数据，而是形成：
- 管理闭环
- 监管闭环
- 风险闭环

---

## 3. 总体方案结论

建议采用：

**方案 A：独立双 App**
- 工人端 App：服务一线工人
- 监管端 App：服务管理与监管角色
- 共用统一后端、统一账号体系、统一组织体系、统一权限中心
- Android 端复用基础能力，不直接复制工人端业务页面

该方案的优势：
1. 职责清晰，页面与交互边界明确
2. 监管业务扩展时不会污染工人端
3. 权限控制更容易做对
4. 独立发布和测试，风险更低
5. 更适合中大型企业级项目长期演进

---

## 4. 双 App 总体业务架构

```text
                           ┌─────────────────────────────┐
                           │        统一后端平台         │
                           │  用户/组织/权限/项目/消息   │
                           └─────────────┬───────────────┘
                                         │
                  ┌──────────────────────┼──────────────────────┐
                  │                                              │
        ┌─────────▼─────────┐                         ┌──────────▼──────────┐
        │      工人端 App    │                         │     监管端 App       │
        │ Worker Client      │                         │ Supervision Client   │
        ├───────────────────┤                         ├─────────────────────┤
        │ 我的任务           │                         │ 首页看板             │
        │ 我的考勤           │                         │ 人员监管             │
        │ 我的工单           │                         │ 考勤监管             │
        │ 我的审批/申请      │                         │ 巡检与隐患管理       │
        │ 我的资料/消息      │                         │ 工单监管             │
        └─────────┬─────────┘                         │ 审批中心             │
                  │                                   │ 消息预警             │
                  │                                   │ 设备告警/统计报表    │
                  │                                   └──────────┬──────────┘
                  │                                              │
                  └──────────────────────┬───────────────────────┘
                                         │
                           ┌─────────────▼─────────────┐
                           │      公共基础能力层        │
                           │ 登录鉴权/网络/上传/定位/  │
                           │ 地图/消息/权限/缓存/日志   │
                           └───────────────────────────┘
```

---

## 5. 监管端完整功能架构

```text
监管端 App
├── A. 登录与权限
│   ├── 账号登录
│   ├── Token 管理
│   ├── 角色识别
│   ├── 菜单权限
│   └── 数据范围控制
│
├── B. 首页看板
│   ├── 项目/组织切换
│   ├── 核心指标统计
│   ├── 风险预警
│   ├── 待办事项
│   ├── 趋势图表
│   └── 快捷功能入口
│
├── C. 人员监管
│   ├── 人员列表
│   ├── 人员详情
│   ├── 实名/证件信息
│   ├── 班组归属
│   ├── 在线状态
│   ├── 人员轨迹/定位
│   └── 人员异常记录
│
├── D. 考勤监管
│   ├── 出勤总览
│   ├── 打卡记录
│   ├── 未打卡/迟到/异常
│   ├── 班组考勤统计
│   ├── 项目考勤统计
│   └── 补卡审批/异常处理
│
├── E. 巡检管理
│   ├── 巡检计划
│   ├── 巡检任务
│   ├── 巡检表单
│   ├── 巡检记录
│   └── 巡检结果统计
│
├── F. 隐患管理
│   ├── 隐患上报
│   ├── 隐患列表
│   ├── 隐患详情
│   ├── 整改指派
│   ├── 整改提交
│   ├── 复查验收
│   ├── 超期预警
│   └── 闭环归档
│
├── G. 工单监管
│   ├── 工单列表
│   ├── 工单分配
│   ├── 工单转派
│   ├── 处理记录
│   ├── 超时工单
│   ├── 工单详情
│   └── 工单关闭/挂起
│
├── H. 审批中心
│   ├── 待我审批
│   ├── 我已审批
│   ├── 我发起的
│   ├── 审批详情
│   ├── 审批轨迹
│   ├── 同意/驳回/转交
│   └── 常见审批类型
│
├── I. 消息预警
│   ├── 系统通知
│   ├── 审批通知
│   ├── 隐患预警
│   ├── 工单预警
│   ├── 设备告警
│   └── 已读未读管理
│
├── J. 设备与告警（可一期弱化）
│   ├── 设备列表
│   ├── 在线状态
│   ├── 告警列表
│   ├── 告警详情
│   ├── 告警处理
│   └── 维保记录
│
├── K. 报表统计（一期可轻量）
│   ├── 出勤统计
│   ├── 隐患整改率
│   ├── 巡检完成率
│   ├── 工单时效
│   ├── 风险趋势
│   └── 项目排行
│
└── L. 我的
    ├── 个人信息
    ├── 角色与组织
    ├── 当前项目切换
    ├── 设置
    ├── 清理缓存
    ├── 关于
    └── 退出登录
```

---

## 6. 监管端核心业务闭环

### 6.1 人员监管闭环
```text
人员入场/建档
   ↓
身份/资质校验
   ↓
班组/项目归属
   ↓
到岗打卡/现场作业
   ↓
出勤与定位监管
   ↓
异常识别（缺勤/异常轨迹/证件到期）
   ↓
通知处理/跟踪整改
```

### 6.2 巡检隐患闭环
```text
巡检计划
   ↓
执行巡检
   ↓
发现隐患
   ↓
隐患上报
   ↓
责任人指派
   ↓
整改处理
   ↓
上传整改凭证
   ↓
复查验收
   ↓
关闭归档
```

### 6.3 工单监管闭环
```text
工单创建
   ↓
分配负责人
   ↓
处理中
   ↓
处理结果提交
   ↓
审核/回退/转派
   ↓
完成关闭
   ↓
时效统计
```

### 6.4 审批闭环
```text
提交申请
   ↓
待审批
   ↓
审批节点流转
   ↓
通过/驳回/转交
   ↓
结果通知
   ↓
归档记录
```

---

## 7. 导航与信息架构建议

### 7.1 底部导航
建议采用 5 个一级入口：
- 首页
- 监管
- 待办
- 消息
- 我的

### 7.2 首页结构
```text
首页
├── 顶部区域
│   ├── 当前组织/项目
│   ├── 搜索入口
│   └── 消息入口
│
├── 核心统计区
│   ├── 今日出勤
│   ├── 未到岗
│   ├── 待整改隐患
│   ├── 待审批
│   ├── 超时工单
│   └── 告警数
│
├── 预警与待办区
│   ├── 风险预警列表
│   ├── 超期事项
│   └── 待办任务
│
├── 快捷入口区
│   ├── 人员
│   ├── 考勤
│   ├── 巡检
│   ├── 隐患
│   ├── 工单
│   ├── 审批
│   └── 报表
│
└── 趋势统计区
    ├── 出勤趋势
    ├── 隐患趋势
    ├── 工单趋势
    └── 项目排行
```

---

## 8. Android 工程模块划分

```text
supervision-app/
├── app
├── common-core
├── common-ui
├── common-network
├── common-storage
├── common-router
├── common-widget
├── common-map
├── common-media
├── common-permission
├── common-message
├── common-web
│
├── feature-login
├── feature-home
├── feature-user-manage
├── feature-attendance
├── feature-inspection
├── feature-hazard
├── feature-workorder
├── feature-approval
├── feature-message
├── feature-device
├── feature-report
├── feature-profile
└── feature-project-selector
```

---

## 9. 各模块职责

### 9.1 app
- Application 初始化
- 全局配置
- 导航宿主 Activity
- 主题与环境切换
- 三方 SDK 初始化

### 9.2 common-core
- BaseActivity / BaseFragment
- BaseViewModel
- UiState / PageState
- 通用异常模型
- Result 封装
- 通用扩展函数
- 协程基础能力

### 9.3 common-ui
- 通用标题栏
- 卡片组件
- 空态/错误态/加载态
- 通用弹窗
- 状态标签
- 表单容器
- 通用按钮风格

### 9.4 common-network
- Retrofit / OkHttp
- Header 注入
- Token 拦截器
- 刷新 Token
- 响应解析
- 错误码处理
- 上传下载能力

### 9.5 common-storage
- DataStore
- 用户信息缓存
- Token 缓存
- 当前项目缓存
- 筛选条件缓存
- Room 本地库

### 9.6 common-router
- 模块跳转协议
- 参数 key 管理
- DeepLink 定义

### 9.7 common-widget
- 日期筛选组件
- 项目选择组件
- 组织树选择组件
- 搜索与筛选组件
- 流程时间轴
- 附件展示组件

### 9.8 common-map
- 地图 SDK 封装
- 定位
- 轨迹展示
- 点位展示
- 围栏/区域展示

### 9.9 common-media
- 拍照
- 相册选择
- 文件选择
- 图片压缩
- 附件上传
- PDF / 视频预览

### 9.10 common-permission
- 相机权限
- 存储权限
- 定位权限
- 通知权限

### 9.11 common-message
- Push 注册
- 本地通知
- 未读数同步
- 消息跳转

### 9.12 common-web
- WebView 容器
- JSBridge
- H5 登录态同步
- 文件上传支持

---

## 10. 业务 Feature 模块定义

### 10.1 feature-login
职责：登录、鉴权、初始化权限与项目上下文

页面：
- LoginActivity / LoginFragment
- 测试环境切换页（仅测试包）

### 10.2 feature-home
职责：首页 Dashboard、统计卡片、风险预警、待办聚合、趋势图表

页面：
- HomeFragment
- 风险预警列表页
- 统计详情页

### 10.3 feature-user-manage
职责：人员列表、详情、证件资质、轨迹、异常记录

页面：
- UserListFragment
- UserDetailFragment
- UserTrackFragment
- CertificateInfoFragment

### 10.4 feature-attendance
职责：出勤总览、打卡记录、异常考勤、未打卡、项目/班组统计

页面：
- AttendanceOverviewFragment
- AttendanceListFragment
- AttendanceAbnormalFragment
- AttendanceDetailFragment

### 10.5 feature-inspection
职责：巡检计划、巡检任务、巡检记录、巡检表单

页面：
- InspectionPlanListFragment
- InspectionTaskListFragment
- InspectionFormFragment
- InspectionRecordDetailFragment

### 10.6 feature-hazard
职责：隐患列表、详情、整改指派、整改提交、复查验收、关闭归档

页面：
- HazardListFragment
- HazardDetailFragment
- HazardAssignFragment
- HazardRectifyFragment
- HazardReviewFragment

### 10.7 feature-workorder
职责：工单列表、详情、分派、转派、挂起、关闭、处理记录

页面：
- WorkOrderListFragment
- WorkOrderDetailFragment
- WorkOrderDispatchFragment
- WorkOrderHandleFragment

### 10.8 feature-approval
职责：待我审批、我已审批、我发起的、审批详情、审批动作、流程轨迹

页面：
- ApprovalTodoFragment
- ApprovalDoneFragment
- ApprovalMineFragment
- ApprovalDetailFragment

### 10.9 feature-message
职责：消息中心、消息详情、预警列表、已读未读管理

页面：
- MessageCenterFragment
- MessageDetailFragment
- WarningListFragment

### 10.10 feature-device
职责：设备列表、告警列表、告警详情、告警处理

页面：
- DeviceListFragment
- DeviceDetailFragment
- DeviceAlarmListFragment
- DeviceAlarmDetailFragment

### 10.11 feature-report
职责：统计图表、趋势分析、项目排行

页面：
- ReportHomeFragment
- AttendanceReportFragment
- HazardReportFragment
- WorkOrderReportFragment

### 10.12 feature-profile
职责：我的、设置、版本、缓存清理、退出登录

页面：
- ProfileFragment
- SettingsFragment
- AboutFragment

### 10.13 feature-project-selector
职责：组织树/项目切换、标段切换、上下文刷新

---

## 11. Android 分层架构

```text
┌────────────────────────────────────────────┐
│                  UI 层                      │
│ Activity / Fragment / Adapter / Dialog     │
│ ViewBinding / RecyclerView / State Render  │
└──────────────────┬─────────────────────────┘
                   │
┌──────────────────▼─────────────────────────┐
│               ViewModel 层                  │
│ 状态管理 / 事件分发 / 页面逻辑编排           │
└──────────────────┬─────────────────────────┘
                   │
┌──────────────────▼─────────────────────────┐
│             Repository / Domain 层          │
│ 业务聚合 / 数据编排 / 缓存策略 / 异常处理     │
└───────────────┬───────────────┬────────────┘
                │               │
      ┌─────────▼──────┐  ┌─────▼──────────┐
      │ Remote Data     │  │ Local Data      │
      │ Retrofit API    │  │ Room/DataStore  │
      └────────────────┘  └─────────────────┘
```

推荐技术栈：
- Kotlin
- MVVM + Repository
- Coroutines + Flow
- Hilt
- Retrofit + OkHttp
- Room + DataStore
- RecyclerView + ListAdapter + DiffUtil
- Paging 3
- ViewBinding

---

## 12. 权限架构设计

### 12.1 权限模型
采用：
- RBAC：控制能不能访问页面和操作按钮
- 数据范围控制：控制能看哪些组织、项目、班组、人员数据

### 12.2 角色建议
- 超级管理员
- 企业管理员
- 项目经理
- 安全员/监管员
- 班组长
- 监理/外部监管人员

### 12.3 前端落地
登录后初始化：
- 用户信息
- 角色信息
- 权限码集合
- 菜单树
- 数据范围

前端职责：
- 动态菜单展示
- 页面入口控制
- 按钮显隐控制
- 无权限兜底页

后端职责：
- 接口强校验
- 数据范围过滤

---

## 13. 与工人端的复用边界

### 13.1 建议复用
- 网络层
- 登录态与 Token 机制
- 上传下载能力
- 定位与地图能力
- 消息推送能力
- 通用 UI 组件
- WebView 容器
- 权限申请封装
- 组织、项目、附件等基础模型

### 13.2 不建议直接复用
- 首页页面
- 列表与详情业务页面
- 工单处理业务逻辑
- 监管端特有 ViewModel 和业务编排

### 13.3 最佳复用策略
- 公共基础能力层：共用
- 公共业务能力层：有限共用
- 业务实现层：工人端与监管端分开实现

---

## 14. 后端接口域划分建议

```text
/auth
  - 登录
  - 刷新 token
  - 获取用户信息
  - 获取权限与菜单

/dashboard
  - 首页统计
  - 风险预警
  - 待办数量
  - 趋势图数据

/user-manage
  - 人员列表
  - 人员详情
  - 证件信息
  - 在线状态
  - 人员轨迹

/attendance
  - 考勤总览
  - 打卡记录
  - 异常考勤
  - 考勤详情

/inspection
  - 巡检计划
  - 巡检任务
  - 巡检记录
  - 巡检表单

/hazard
  - 隐患列表
  - 隐患详情
  - 整改提交
  - 复查
  - 关闭

/workorder
  - 工单列表
  - 工单详情
  - 指派/转派/关闭
  - 处理记录

/approval
  - 审批列表
  - 审批详情
  - 审批动作

/message
  - 消息列表
  - 消息已读
  - 未读数

/device
  - 设备列表
  - 告警列表
  - 告警详情
  - 告警处理

/report
  - KPI 数据
  - 报表趋势
  - 排行统计
```

---

## 15. 一期与二期范围建议

### 15.1 一期范围
建议优先实现：
- 登录与权限
- 首页看板
- 人员监管
- 考勤监管
- 巡检管理
- 隐患管理
- 工单监管
- 审批中心
- 消息预警
- 我的/设置
- 项目切换

### 15.2 一期目标
实现以下闭环：
- 看全局
- 查问题
- 下指令
- 跟处理
- 做审批
- 收预警

### 15.3 二期范围
- 设备与 IoT 告警
- 视频监控接入
- 深度统计报表
- 大屏联动
- AI 风险识别
- 风险评分体系

---

## 16. 页面标准化建议

### 16.1 列表页标准
统一包含：
- 顶部筛选区
- 搜索框
- 状态 Tab
- RecyclerView
- 下拉刷新
- 上拉分页
- 空态/错误态

### 16.2 详情页标准
统一包含：
- 基础信息区
- 状态区
- 时间轴/流转区
- 附件区
- 操作记录区
- 底部操作区

### 16.3 表单页标准
统一包含：
- 说明区
- 表单输入区
- 附件上传区
- 提交按钮区
- 校验提示

---

## 17. 状态管理规范

### 页面状态
- Loading
- Success
- Empty
- Error
- NoPermission
- NoProjectSelected

### 列表状态
- firstLoading
- refreshing
- loadMore
- content
- empty
- error

---

## 18. 研发实施顺序建议

### 第一阶段：工程基础搭建
- 新监管端工程初始化
- 公共模块接入
- 登录权限链路打通
- UI 基础组件搭建
- 项目切换能力接入

### 第二阶段：首页与通用框架
- 首页看板
- 列表页基类
- 详情页基类
- 表单页基类
- 消息中心框架

### 第三阶段：核心业务闭环
- 人员监管
- 考勤监管
- 巡检
- 隐患闭环
- 工单监管
- 审批中心

### 第四阶段：增强能力
- 报表统计
- 设备告警
- 推送联动
- 性能优化

---

## 19. 风险与控制点

### 风险 1：需求膨胀
处理建议：
- 一期只做闭环
- 高复杂低频功能先用 H5 承接

### 风险 2：权限后补
处理建议：
- 从登录初始化就拉菜单、权限码、数据范围
- 前后端同步设计

### 风险 3：复制工人端业务代码
处理建议：
- 只复用基础能力
- 页面和业务编排独立开发

### 风险 4：首页失焦
处理建议：
- 首页采用“统计 + 预警 + 待办 + 快捷入口”结构
- 不做纯九宫格工具页

---

## 20. 最终架构结论

### 方案结论
监管端 App 应采用：
- 独立双 App 形态
- 统一后端、统一账号、统一权限中心
- Android 端采用 Kotlin + MVVM + Repository + Hilt
- 基础能力复用，业务页面独立
- 一期围绕核心闭环建设

### 一期重点模块
- 首页看板
- 人员监管
- 考勤监管
- 巡检与隐患闭环
- 工单监管
- 审批中心
- 消息预警
- 项目切换

---

## 21. 下一步建议

建议下一阶段直接产出以下内容：
1. 一期页面清单
2. 接口清单
3. 角色权限表
4. Android 工程目录落地版
5. 模块开发顺序与排期建议

本文档完成后，可作为监管端一期立项与技术方案基线文档使用。

