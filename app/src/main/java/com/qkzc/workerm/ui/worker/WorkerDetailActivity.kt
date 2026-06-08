package com.qkzc.workerm.ui.worker

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.data.worker.ManagerWorker
import com.qkzc.workerm.data.worker.ManagerWorkerRepository
import com.qkzc.workerm.databinding.ActivityWorkerDetailBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class WorkerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerDetailBinding
    private val workerRepository = ManagerWorkerRepository()

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
                val projectId = intent.getLongExtra(EXTRA_PROJECT_ID, 0L)
                    .takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                val workerId = intent.getLongExtra(EXTRA_WORKER_ID, 0L)
                    .takeIf { it > 0L }
                    ?: error("请从工人列表或扫码结果进入详情")
                workerRepository.detail(session.accessToken, projectId, workerId)
            }.onSuccess { worker ->
                bindWorker(worker)
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
        binding.entryProgressTitleText.text = "入场流程（${completedStepCount(worker)}/5）"
        setFlowSteps(worker)
    }

    private fun setFlowSteps(worker: ManagerWorker?) {
        binding.identityStatusText.text = flowStep("身份认证", worker?.identityStatus)
        binding.safetyStatusText.text = flowStep("安全培训", worker?.safetyTrainingStatus)
        binding.healthStatusText.text = flowStep("健康检查", worker?.healthCheckStatus)
        binding.contractStatusText.text = flowStep("合同签署", worker?.entryContractStatus ?: worker?.contractStatus)
        binding.insuranceStatusText.text = flowStep("保险签署", worker?.insuranceStatus)
    }

    private fun completedStepCount(worker: ManagerWorker): Int {
        return listOf(
            worker.identityStatus,
            worker.safetyTrainingStatus,
            worker.healthCheckStatus,
            worker.entryContractStatus.ifBlank { worker.contractStatus },
            worker.insuranceStatus,
        ).count { isCompleted(it) }
    }

    private fun flowStep(label: String, status: String?): String {
        val completed = isCompleted(status)
        val marker = if (completed) "✓" else "·"
        return "$marker\n$label"
    }

    private fun isCompleted(status: String?): Boolean {
        return when (status.orEmpty().uppercase(Locale.getDefault())) {
            "COMPLETED", "PASS", "PASSED", "SIGNED", "DONE", "1", "TRUE" -> true
            else -> false
        }
    }

    private fun statusLabel(status: String): String {
        return when (status.uppercase(Locale.getDefault())) {
            "ACTIVE" -> "在场"
            "BOUND" -> "已绑定"
            "ENTERING" -> "入场中"
            "EXITED" -> "已退场"
            "CANCELLED" -> "已取消"
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

    companion object {
        const val EXTRA_PROJECT_ID = "projectId"
        const val EXTRA_WORKER_ID = "workerId"
    }
}
