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
import com.qkzc.workerm.databinding.ActivityWorkerScanBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkerScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerScanBinding
    private val workerRepository = ManagerWorkerRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWorkerScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.scanRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        loadWorker()
    }

    private fun loadWorker() {
        lifecycleScope.launch {
            binding.scanStatusText.text = "正在校验"
            runCatching {
                val session = SessionStore(this@WorkerScanActivity).sessionFlow.first()
                val projectId = intent.getLongExtra(EXTRA_PROJECT_ID, 0L)
                    .takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                val workerId = intent.getLongExtra(EXTRA_WORKER_ID, 0L)
                    .takeIf { it > 0L }
                    ?: workerRepository.listWorkers(session.accessToken, projectId).firstOrNull()?.workerUserId
                    ?: error("当前项目暂无可扫码查看的工人")
                workerRepository.scanTicket(session.accessToken, projectId, workerId)
            }.onSuccess { worker ->
                bindWorker(worker)
                binding.scanStatusText.text = "扫描成功"
            }.onFailure { throwable ->
                binding.scanStatusText.text = "校验失败"
                binding.workerNameText.text = "暂无工人"
                binding.workerTypeText.text = throwable.message ?: "工人信息加载失败"
                binding.workerLeaderText.text = "请确认二维码和项目范围"
                binding.workerDetailText.text = ""
            }
        }
    }

    private fun bindWorker(worker: ManagerWorker) {
        binding.workerNameText.text = worker.realName.ifBlank { "未命名工人" }
        binding.workerTypeText.text = worker.workTypeName.ifBlank { "未配置工种" }
        binding.workerLeaderText.text = "所属班组：${worker.leaderName.ifBlank { "未绑定班组" }}"
        binding.workerDetailText.text = buildString {
            append("入场时间：")
            append(worker.createTime.ifBlank { "暂无" })
            append('\n')
            append("身份证号：")
            append(maskIdCard(worker.idCardNo))
            append('\n')
            append("所属项目：")
            append(worker.projectName.ifBlank { "暂无" })
        }
        binding.scanTimeText.text = "扫描时间：${nowText()}        继续扫码 >"
    }

    private fun maskIdCard(idCardNo: String): String {
        if (idCardNo.length < 8) return "暂无"
        return idCardNo.take(3) + "************" + idCardNo.takeLast(4)
    }

    private fun nowText(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date())
    }

    companion object {
        const val EXTRA_PROJECT_ID = "projectId"
        const val EXTRA_WORKER_ID = "workerId"
    }
}
