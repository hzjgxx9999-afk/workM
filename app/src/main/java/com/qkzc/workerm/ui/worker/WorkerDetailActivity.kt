package com.qkzc.workerm.ui.worker

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.data.worker.ManagerWorker
import com.qkzc.workerm.data.worker.ManagerWorkerRepository
import com.qkzc.workerm.data.worker.entryProgressText
import com.qkzc.workerm.data.worker.entrySteps
import com.qkzc.workerm.databinding.ActivityWorkerDetailBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class WorkerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerDetailBinding
    private val workerRepository = ManagerWorkerRepository()
    private var currentAccessToken: String = ""
    private var currentProjectId: Long = 0L
    private var currentWorkerId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWorkerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.workerDetailRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        bindLoading()
        loadWorker()
    }

    private fun loadWorker() {
        lifecycleScope.launch {
            runCatching {
                val session = SessionStore(this@WorkerDetailActivity).sessionFlow.first()
                currentAccessToken = session.accessToken
                val projectId = intent.getLongExtra(EXTRA_PROJECT_ID, 0L)
                    .takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                val workerId = intent.getLongExtra(EXTRA_WORKER_ID, 0L)
                    .takeIf { it > 0L }
                    ?: error("请从工人列表或扫码结果进入详情")
                currentProjectId = projectId
                currentWorkerId = workerId
                workerRepository.detail(session.accessToken, projectId, workerId)
            }.onSuccess { worker ->
                bindWorker(worker)
                loadRelationHistory()
            }.onFailure { throwable ->
                bindError(throwable.message ?: "工人信息加载失败")
            }
        }
    }

    private fun bindLoading() {
        binding.workerNameText.text = "加载中"
        binding.workerTypeText.text = "正在读取工人信息"
        binding.workerStatusText.text = "--"
        binding.workerTeamText.text = "所属班组\n--"
        binding.workerLeaderText.text = "班组长\n--"
        binding.workerProjectText.text = "项目名称\n--"
        binding.workerMobileText.text = "手机号\n--"
        binding.workerIdCardText.text = "身份证号\n--"
        binding.entryProgressTitleText.text = "入场流程"
        setFlowSteps(null)
    }

    private fun bindError(message: String) {
        binding.workerNameText.text = "暂无工人"
        binding.workerTypeText.text = message
        binding.workerStatusText.text = "异常"
        binding.workerTeamText.text = "所属班组\n--"
        binding.workerLeaderText.text = "班组长\n--"
        binding.workerProjectText.text = "项目名称\n--"
        binding.workerMobileText.text = "手机号\n--"
        binding.workerIdCardText.text = "身份证号\n--"
        binding.entryProgressTitleText.text = "入场流程"
        setFlowSteps(null)
    }

    private fun loadRelationHistory() {
        val token = currentAccessToken.takeIf { it.isNotBlank() } ?: return
        val workerId = currentWorkerId.takeIf { it > 0L } ?: return
        val projectId = currentProjectId.takeIf { it > 0L }
        lifecycleScope.launch {
            runCatching {
                workerRepository.relationHistory(token, workerId, projectId)
            }.onSuccess { relations ->
                val text = relations.joinToString("\n\n") { relation ->
                    listOf(
                        "项目：${relation.projectName.ifBlank { "--" }}",
                        "班组长：${relation.leaderName.ifBlank { "--" }}",
                        "班组：${relation.teamName.ifBlank { "--" }}",
                        "状态：${statusLabel(relation.status)}",
                        "绑定时间：${relation.bindTime.ifBlank { "--" }}"
                    ).joinToString("\n")
                }.ifBlank { "暂无项目履历" }
                bindRelationHistory(text)
            }.onFailure {
                bindRelationHistory("项目履历加载失败")
            }
        }
    }

    private fun bindRelationHistory(text: String) {
        val container = binding.workerDetailRoot.getChildAt(0) as? LinearLayout ?: return
        val existing = container.findViewWithTag<TextView>("relation_history_card")
        val card = existing ?: TextView(this).apply {
            tag = "relation_history_card"
            setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
            textSize = 13f
            setTextColor(getColor(com.qkzc.workerm.R.color.text_primary))
            background = getDrawable(com.qkzc.workerm.R.drawable.bg_card_stroke)
            container.addView(this, ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 12.dp()
            })
        }
        card.text = "项目履历\n$text"
    }

    private fun bindWorker(worker: ManagerWorker) {
        val status = worker.bindStatus.ifBlank { worker.entryStatus }
        binding.workerNameText.text = worker.realName.ifBlank { "未命名工人" }
        binding.workerTypeText.text = worker.workTypeName.ifBlank { "未配置工种" }
        binding.workerStatusText.text = statusLabel(status)
        binding.workerTeamText.text = "所属班组\n${worker.teamName.ifBlank { "暂无" }}"
        binding.workerLeaderText.text = "班组长\n${worker.leaderName.ifBlank { "暂无" }}"
        binding.workerProjectText.text = "项目名称\n${worker.projectName.ifBlank { "暂无" }}"
        binding.workerMobileText.text = "手机号\n${maskMobile(worker.mobile)}"
        binding.workerIdCardText.text = "身份证号\n${maskIdCard(worker.idCardNo)}"
        binding.entryProgressTitleText.text = worker.entryProgressText()
        setFlowSteps(worker)
    }

    private fun setFlowSteps(worker: ManagerWorker?) {
        val steps = worker?.entrySteps().orEmpty()
        binding.identityStatusText.text = steps.getOrNull(0)?.displayText ?: defaultFlowStep("身份认证")
        binding.safetyStatusText.text = steps.getOrNull(1)?.displayText ?: defaultFlowStep("安全培训")
        binding.healthStatusText.text = steps.getOrNull(2)?.displayText ?: defaultFlowStep("健康检查")
        binding.contractStatusText.text = steps.getOrNull(3)?.displayText ?: defaultFlowStep("合同签署")
        binding.insuranceStatusText.text = steps.getOrNull(4)?.displayText ?: defaultFlowStep("保险签署")
    }

    private fun defaultFlowStep(label: String): String {
        return "·\n$label\n未开始"
    }

    private fun statusLabel(status: String): String {
        return when (status.uppercase(Locale.ROOT)) {
            "ACTIVE" -> "在场"
            "BOUND" -> "已绑定"
            "ENTERING" -> "入场中"
            "EXITED", "LEFT" -> "已退场"
            "CANCELLED" -> "已取消"
            "REJECTED" -> "已驳回"
            "ABNORMAL" -> "异常"
            "COMPLETED" -> "完成"
            else -> status.ifBlank { "--" }
        }
    }

    private fun maskMobile(mobile: String): String {
        val text = mobile.trim()
        if (text.isEmpty()) return "暂无"
        if (text.contains("*") || text.length < 7) return text
        return text.take(3) + "****" + text.takeLast(4)
    }

    private fun maskIdCard(idCardNo: String): String {
        val text = idCardNo.trim()
        if (text.isEmpty()) return "暂无"
        if (text.contains("*") || text.length < 8) return text
        return text.take(3) + "************" + text.takeLast(4)
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_PROJECT_ID = "projectId"
        const val EXTRA_WORKER_ID = "workerId"
    }
}
