package com.qkzc.workerm.ui.dispatch

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.R
import com.qkzc.workerm.data.dispatch.DispatchAcceptanceDetail
import com.qkzc.workerm.data.dispatch.DispatchLog
import com.qkzc.workerm.data.dispatch.DispatchManualProcessNodeReq
import com.qkzc.workerm.data.dispatch.DispatchRepository
import com.qkzc.workerm.data.dispatch.DispatchRealtimeClient
import com.qkzc.workerm.data.dispatch.DispatchSpotCheckSummary
import com.qkzc.workerm.data.dispatch.DispatchStatusPresentation
import com.qkzc.workerm.data.project.ManagerProjectTeam
import com.qkzc.workerm.data.project.ProjectTeamRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityDispatchDetailBinding
import com.qkzc.workerm.ui.common.EdgeToEdgeActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DispatchDetailActivity : EdgeToEdgeActivity() {
    private lateinit var binding: ActivityDispatchDetailBinding
    private val repository = DispatchRepository()
    private val teamRepository = ProjectTeamRepository()
    private val sessionStore by lazy { SessionStore(applicationContext) }
    private var detail = DispatchAcceptanceDetail()
    private val workerAdapter = DispatchWorkerResultAdapter(::openPhotos)
    private val processAdapter = DispatchProcessCheckAdapter(::openPhotos)
    private val evidenceAdapter = DispatchEvidenceGroupAdapter(::openPhotos)
    private val reviewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { loadDetail() }
    private var detailLoadJob: Job? = null
    private var pollingJob: Job? = null
    private val realtimeClient = DispatchRealtimeClient(::loadDetail)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDispatchDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdge(
            root = binding.root,
            topBarId = R.id.dispatch_detail_top_bar,
            bottomBarId = R.id.action_bar,
        )
        configureSystemBarIconAppearance(
            lightStatusBars = true,
            lightNavigationBars = true,
        )
        binding.workerList.layoutManager = LinearLayoutManager(this)
        binding.workerList.adapter = workerAdapter
        binding.processList.layoutManager = LinearLayoutManager(this)
        binding.processList.adapter = processAdapter
        binding.evidenceList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.evidenceList.adapter = evidenceAdapter
        binding.backButton.setOnClickListener { finish() }
        binding.menuButton.setOnClickListener { showMenu() }
        binding.approveButton.setOnClickListener { approve() }
        binding.rejectButton.setOnClickListener { reject() }
        binding.spotCheckAction.setOnClickListener { handleSpotCheckAction() }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val token = sessionStore.sessionFlow.first().accessToken
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) realtimeClient.start(token)
        }
        pollingJob?.cancel()
        pollingJob = lifecycleScope.launch {
            while (isActive) {
                delay(10_000L)
                loadDetail()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) loadDetail()
    }

    override fun onStop() {
        realtimeClient.stop()
        pollingJob?.cancel()
        pollingJob = null
        super.onStop()
    }

    private fun loadDetail() {
        val id = intent.getLongExtra(EXTRA_ID, 0L)
        if (id <= 0L) { toast("派工单参数错误"); finish(); return }
        detailLoadJob?.cancel()
        detailLoadJob = lifecycleScope.launch {
            runCatching {
                val token = sessionStore.sessionFlow.first().accessToken
                repository.acceptanceDetail(token, id)
            }.onSuccess { detail = it; render() }
                .onFailure {
                    if (it is CancellationException) throw it
                    toast(it.message ?: "派工验收详情加载失败")
                }
        }
    }

    private fun render() = with(binding) {
        val order = detail.order
        val managerRejectedSummary = order.status == "PENDING_TEAM_REWORK"
        statusText.text = if (managerRejectedSummary) "待原班组整改" else DispatchStatusPresentation.label(order.status, order.overdue)
        titleText.text = order.title.ifBlank { "未命名派工单" }
        phaseText.text = if (managerRejectedSummary) "项目经理已驳回，等待原班组整改后重新提交" else phaseDescription(order.status)
        dispatchNoText.text = "派工单编号：${order.dispatchNo.ifBlank { "-" }}"
        projectText.text = "所属项目：${order.projectName.ifBlank { "-" }}"
        locationText.text = "施工位置：${order.locationDesc.ifBlank { "-" }}"
        deadlineText.text = "截止时间：${order.deadlineTime.ifBlank { "-" }}"
        teamText.text = "派工班组：${order.teamName.ifBlank { "-" }}"
        leaderText.text = "班组长：${order.leaderName.ifBlank { "-" }}"
        requirementContentText.text = order.content.ifBlank { "暂无任务说明" }
        requirementConstructionText.text = "施工要求：${order.constructionRequirement.orEmpty().ifBlank { "未设置" }}"
        requirementSafetyText.text = "安全注意：${order.safetyNotice.orEmpty().ifBlank { "未设置" }}"
        requirementStandardText.text = "验收标准：${order.acceptanceStandard.orEmpty().ifBlank { "未设置" }}"
        summaryTimeText.text = "提交时间：${detail.summaryTime.orEmpty().ifBlank { "尚未提交" }}"
        summaryContentText.text = detail.leaderSummary.orEmpty().ifBlank {
            if (managerRejectedSummary) "原班组汇总已驳回，请查看整改意见和历史照片后重新施工并提交。"
            else "班组暂未提交汇总"
        }
        timelineText.text = detail.timeline
            .sortedBy { it.id }
            .filterNot(::isPhotoUploadTimelineLog)
            .joinToString("\n\n") { log ->
                val actor = log.operatorName.ifBlank { "系统" }
                val time = log.createTime.orEmpty().ifBlank { "时间待同步" }
                val remark = timelineRemark(log)?.let { "\n   $it" }.orEmpty()
                "● $time  $actor · ${timelineActionLabel(log.action)}$remark"
            }
            .ifBlank { "暂无操作记录" }
        workerAdapter.submit(detail.workers)
        processAdapter.submit(detail.processChecks)
        evidenceAdapter.submit(detail.evidenceGroups)

        val latestSpot = detail.spotChecks.firstOrNull()
        spotCheckBanner.visibility = if (latestSpot != null || detail.permissions.canCreateSpotCheck) View.VISIBLE else View.GONE
        spotCheckStatusText.text = latestSpot?.let {
            "抽查：${it.nodeName} · ${DispatchStatusPresentation.label(it.status)}${if (it.overdue) " · 已逾期" else ""}"
        } ?: "当前未发起项目经理抽查"
        spotCheckAction.text = if (latestSpot == null) "发起抽查" else "查看抽查"

        acceptanceSection.visibility = if (order.status == "PENDING_MANAGER_ACCEPTANCE") View.VISIBLE else View.GONE
        actionBar.visibility = if (detail.permissions.canReject || detail.permissions.canApprove) View.VISIBLE else View.GONE
        approveButton.isEnabled = detail.permissions.canApprove
        if (!detail.permissions.canApprove && detail.permissions.approvalBlockedReason == "SPOT_CHECK_PENDING") {
            approveButton.text = "抽查完成后验收"
        } else approveButton.text = "验收通过"
        rejectButton.isEnabled = detail.permissions.canReject
    }

    private fun phaseDescription(status: String) = when (status) {
        "PENDING_MANAGER_ACCEPTANCE" -> "班组已提交，等待项目经理验收"
        "PENDING_PROCESS_CHECK" -> "施工资料已提交，等待过程检查"
        "PENDING_WORKER_RESULT_REVIEW" -> "工人已提交，等待班组长审核"
        "PENDING_LEADER_SUMMARY" -> "等待班组长汇总"
        "PENDING_TEAM_REWORK" -> "项目经理已驳回，等待原班组整改并重新施工"
        "COMPLETED" -> "派工验收已完成"
        else -> "派工任务执行中"
    }

    private fun timelineActionLabel(action: String) = when (action) {
        "CREATE" -> "创建并派工"
        "LEADER_ACCEPT", "LEADER_ACCEPT_SYNC" -> "班组长接收"
        "LEADER_DECLINE" -> "班组长退回"
        "ASSIGN_WORKERS" -> "分派工人"
        "CREATE_SPOT_CHECK" -> "发起项目经理抽查"
        "SUBMIT_SPOT_CHECK" -> "提交抽查资料"
        "CANCEL_SPOT_CHECK" -> "取消项目经理抽查"
        "ACCEPT" -> "工人接收"
        "WORKER_DECLINE" -> "工人拒绝"
        "WORKER_UPLOAD_BEFORE_LATE" -> "工人开工后补录施工前照片"
        "START" -> "开始施工"
        "PROCESS_SUBMIT" -> "提交过程资料"
        "PROCESS_REVIEW_PASS" -> "过程审核通过"
        "PROCESS_REVIEW_REJECT" -> "过程资料驳回"
        "WORKER_SUBMIT" -> "提交完工结果"
        "LEADER_SELF_SUBMIT" -> "班组长本人提交完工结果"
        "WORKER_RESULT_PASS" -> "工人结果通过"
        "WORKER_RESULT_REJECT" -> "工人结果驳回"
        "LEADER_SUMMARY" -> "提交班组汇总"
        "APPROVE" -> "项目验收通过"
        "REJECT" -> "项目验收驳回"
        "REASSIGN_LEADER" -> "重新指派班组"
        "CANCEL" -> "取消派工单"
        else -> action.replace('_', ' ').ifBlank { "状态更新" }
    }

    private fun isPhotoUploadTimelineLog(log: DispatchLog): Boolean {
        val action = log.action.trim().uppercase(Locale.ROOT)
        val photoUploadAction = action.contains("UPLOAD") && listOf(
            "PHOTO",
            "IMAGE",
            "PICTURE",
            "CHECK_NODE",
            "BEFORE_LATE",
        ).any(action::contains)
        return photoUploadAction || IMAGE_FILE_NAME_REGEX.containsMatchIn(log.remark.orEmpty())
    }

    private fun timelineRemark(log: DispatchLog): String? =
        FILE_NAME_REGEX.replace(log.remark.orEmpty(), "")
            .replace(Regex("[ \\t]{2,}"), " ")
            .trim()
            .trimEnd(';', '；', ',', '，', '、', ':', '：')
            .takeIf(String::isNotBlank)

    private fun handleSpotCheckAction() {
        val spot = detail.spotChecks.firstOrNull()
        if (spot == null) showCreateSpotCheckDialog()
        else reviewLauncher.launch(SpotCheckReviewActivity.intent(this, detail.order.id, spot.nodeId))
    }

    private fun showCreateSpotCheckDialog() {
        if (!detail.permissions.canCreateSpotCheck) { toast("当前存在未完成抽查或工单状态不允许发起抽查"); return }
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 8, 48, 0) }
        val name = EditText(this).apply { hint = "抽查节点名称"; setText("项目经理现场抽查") }
        val description = EditText(this).apply { hint = "抽查要求说明"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; minLines = 2 }
        val deadline = EditText(this).apply {
            hint = "抽查时限 yyyy-MM-dd HH:mm:ss"
            setText(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)))
        }
        container.addView(name); container.addView(description); container.addView(deadline)
        val dialog = AlertDialog.Builder(this).setTitle("发起项目经理抽查").setView(container)
            .setNegativeButton("取消", null).setPositiveButton("确认发起", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nodeName = name.text?.toString()?.trim().orEmpty()
                val deadlineText = deadline.text?.toString()?.trim().orEmpty()
                val validDeadline = runCatching { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).apply { isLenient = false }.parse(deadlineText) }.getOrNull()
                when {
                    nodeName.isBlank() -> name.error = "请输入抽查名称"
                    validDeadline == null || validDeadline.time <= System.currentTimeMillis() -> deadline.error = "抽查时限必须晚于当前时间"
                    else -> { dialog.dismiss(); createSpotCheck(nodeName, description.text?.toString()?.trim(), deadlineText) }
                }
            }
        }
        dialog.show()
    }

    private fun createSpotCheck(name: String, description: String?, deadline: String) = lifecycleScope.launch {
        runCatching {
            val token = sessionStore.sessionFlow.first().accessToken
            repository.createSpotCheck(token, detail.order.id, DispatchManualProcessNodeReq(
                nodeName = name,
                requiredPhotoCount = detail.order.processMinPhotoCount.coerceAtLeast(1).coerceAtMost(9),
                requiredLocation = detail.order.processRequireLocation,
                description = description,
                submitDeadline = deadline,
            ))
        }.onSuccess { toast("已发起抽查"); loadDetail() }
            .onFailure { toast(it.message ?: "发起抽查失败") }
    }

    private fun approve() {
        val comment = binding.acceptanceInput.text?.toString()?.trim().orEmpty()
        if (comment.length < 10) { binding.acceptanceInput.error = "验收通过意见至少10个字"; return }
        audit(true, comment)
    }

    private fun reject() {
        val comment = binding.acceptanceInput.text?.toString()?.trim().orEmpty()
        if (comment.isBlank()) { binding.acceptanceInput.error = "请填写驳回整改原因"; return }
        audit(false, comment)
    }

    private fun audit(approve: Boolean, comment: String) = lifecycleScope.launch {
        binding.approveButton.isEnabled = false; binding.rejectButton.isEnabled = false
        runCatching {
            val token = sessionStore.sessionFlow.first().accessToken
            if (approve) repository.approve(token, detail.order.id, detail.order.version, comment)
            else repository.reject(token, detail.order.id, detail.order.version, comment)
        }.onSuccess { toast(if (approve) "验收已通过" else "已驳回整改"); loadDetail() }
            .onFailure { toast(it.message ?: "验收操作失败") }
        binding.approveButton.isEnabled = detail.permissions.canApprove; binding.rejectButton.isEnabled = detail.permissions.canReject
    }

    private fun showMenu() {
        PopupMenu(this, binding.menuButton).apply {
            if (detail.permissions.canCreateSpotCheck && detail.spotChecks.isNotEmpty()) menu.add("再次发起抽查")
            if (detail.permissions.canReassign) menu.add("更换班组")
            if (detail.permissions.canCancel) menu.add("取消派工单")
            setOnMenuItemClickListener {
                when (it.title.toString()) {
                    "再次发起抽查" -> showCreateSpotCheckDialog()
                    "更换班组" -> showReassignDialog()
                    "取消派工单" -> showReasonDialog("取消派工单", "请输入取消原因") { reason -> cancel(reason) }
                }; true
            }
            show()
        }
    }

    private fun showReassignDialog() = lifecycleScope.launch {
        runCatching {
            val token = sessionStore.sessionFlow.first().accessToken
            token to teamRepository.loadTeams(token, detail.order.projectId).filter { it.enabled && it.leaderId > 0L && it.teamId != detail.order.teamId }
        }.onSuccess { (token, teams) ->
            if (teams.isEmpty()) { toast("当前项目没有其他可用班组"); return@onSuccess }
            AlertDialog.Builder(this@DispatchDetailActivity).setTitle("更换班组")
                .setItems(teams.map { "${it.teamName} - ${it.leaderName}" }.toTypedArray()) { _, which -> reassign(token, teams[which]) }
                .setNegativeButton("取消", null).show()
        }.onFailure { toast(it.message ?: "班组列表加载失败") }
    }

    private fun reassign(token: String, team: ManagerProjectTeam) = lifecycleScope.launch {
        runCatching { repository.reassign(token, detail.order.id, team.teamId, team.leaderId, detail.order.version) }
            .onSuccess { toast("班组已更换"); loadDetail() }.onFailure { toast(it.message ?: "更换班组失败") }
    }

    private fun cancel(reason: String) = lifecycleScope.launch {
        runCatching {
            val token = sessionStore.sessionFlow.first().accessToken
            repository.cancel(token, detail.order.id, detail.order.version, reason)
        }.onSuccess { toast("派工单已取消"); loadDetail() }.onFailure { toast(it.message ?: "取消失败") }
    }

    private fun showReasonDialog(title: String, hint: String, onConfirm: (String) -> Unit) {
        val input = EditText(this).apply { this.hint = hint }
        val dialog = AlertDialog.Builder(this).setTitle(title).setView(input).setNegativeButton("取消", null).setPositiveButton("确认", null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val value = input.text?.toString()?.trim().orEmpty()
            if (value.isBlank()) input.error = "原因不能为空" else { dialog.dismiss(); onConfirm(value) }
        } }
        dialog.show()
    }

    private fun openPhotos(urls: List<String>) { if (urls.isNotEmpty()) startActivity(DispatchPhotoPreviewActivity.intent(this, urls)) }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        private const val EXTRA_ID = "dispatch_id"
        private val IMAGE_FILE_NAME_REGEX = Regex(
            """(?i)[\p{L}\p{N}_-][\p{L}\p{N}_.-]*\.(?:jpe?g|png|webp|gif|heic|bmp)\b""",
        )
        private val FILE_NAME_REGEX = Regex(
            """(?i)[\p{L}\p{N}_-][\p{L}\p{N}_.-]*\.(?:jpe?g|png|webp|gif|heic|bmp|pdf|docx?|xlsx?|zip|mp4|mov)\b""",
        )
        fun intent(context: Context, id: Long) = Intent(context, DispatchDetailActivity::class.java).putExtra(EXTRA_ID, id)
    }
}
