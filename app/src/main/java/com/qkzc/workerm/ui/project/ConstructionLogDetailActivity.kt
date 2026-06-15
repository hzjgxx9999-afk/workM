package com.qkzc.workerm.ui.project

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.R
import com.qkzc.workerm.data.project.ConstructionLogDetail
import com.qkzc.workerm.data.project.ConstructionLogRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityConstructionLogDetailBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConstructionLogDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConstructionLogDetailBinding
    private lateinit var sessionStore: SessionStore
    private val repository = ConstructionLogRepository()
    private val attachmentAdapter = ConstructionLogAttachmentAdapter(editable = false)

    private var currentProjectId: Long = 0L
    private var currentLogId: Long = 0L
    private var currentDetail: ConstructionLogDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityConstructionLogDetailBinding.inflate(layoutInflater)
        sessionStore = SessionStore(applicationContext)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.detailRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        binding.editButton.setOnClickListener { currentDetail?.let { openEditor(it.id) } }
        binding.submitButton.setOnClickListener { currentDetail?.let { submit(it) } }
        binding.deleteButton.setOnClickListener { currentDetail?.let { delete(it) } }
        binding.withdrawButton.setOnClickListener { currentDetail?.let { withdraw(it) } }
        binding.attachmentRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.attachmentRecyclerView.adapter = attachmentAdapter
        currentProjectId = intent.getLongExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, 0L)
        currentLogId = intent.getLongExtra(EXTRA_LOG_ID, 0L)
        loadDetail()
    }

    override fun onResume() {
        super.onResume()
        if (currentLogId > 0L) {
            loadDetail()
        }
    }

    private fun loadDetail() {
        if (currentLogId <= 0L) {
            toast("缺少日志参数")
            finish()
            return
        }
        lifecycleScope.launch {
            runCatching {
                val session = sessionStore.sessionFlow.first()
                val projectId = currentProjectId.takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                repository.detail(session.accessToken, projectId, currentLogId)
            }.onSuccess { detail ->
                currentDetail = detail
                render(detail)
            }.onFailure { throwable ->
                toast(throwable.message ?: "日志详情加载失败")
                finish()
            }
        }
    }

    private fun render(detail: ConstructionLogDetail) {
        binding.statusText.text = detail.status
        binding.titleText.text = detail.title.ifBlank { "施工日志详情" }
        binding.baseInfoText.text = buildString {
            append("日志编号：")
            append(detail.logNo.ifBlank { "-" })
            append('\n')
            append("日志日期：")
            append(detail.logDate.ifBlank { "-" })
            append('\n')
            append("施工区域：")
            append(detail.sectionName.ifBlank { "-" })
            append('\n')
            append("班组：")
            append(detail.teamName.ifBlank { "-" })
            append('\n')
            append("记录人：")
            append(detail.recorderName.ifBlank { "-" })
        }
        binding.summaryText.text = buildString {
            append("天气 ${detail.weatherAm.ifBlank { "-" }}/${detail.weatherPm.ifBlank { "-" }}")
            append("    人数 ${detail.workerCount} 人")
            append("    机械 ${detail.machineCount} 台")
            append('\n')
            append("隐患数 ${detail.riskCount}")
            if (detail.locationText.isNotBlank()) {
                append("    位置 ")
                append(detail.locationText)
            }
        }
        binding.progressText.text = detail.progressSummary.ifBlank { "暂无进度说明" }
        binding.qualityText.text = detail.qualitySummary.ifBlank { "暂无质量情况" }
        binding.safetyText.text = detail.safetySummary.ifBlank { "暂无安全情况" }
        binding.contentText.text = detail.contentText.ifBlank { "暂无施工记录内容" }
        binding.remarkText.text = buildString {
            append("整改建议：")
            append(detail.rectifySuggestion.ifBlank { "无" })
            append('\n')
            append("备注：")
            append(detail.extraRemark.ifBlank { "无" })
        }
        attachmentAdapter.submitList(detail.attachments)
        binding.emptyAttachmentText.isVisible = detail.attachments.isEmpty()
        renderTracks(detail)
        renderVersions(detail)
        renderActions(detail)
    }

    private fun renderTracks(detail: ConstructionLogDetail) {
        binding.auditTrackContainer.removeAllViews()
        if (detail.auditTracks.isEmpty()) {
            binding.auditTrackContainer.addView(simpleText("暂无审核轨迹"))
            return
        }
        detail.auditTracks.forEach { track ->
            binding.auditTrackContainer.addView(
                simpleText(
                    "节点${track.nodeNo} ${track.approverName.ifBlank { "-" }}  ${track.status.ifBlank { "-" }}\n" +
                        "${track.action.ifBlank { "待处理" }}  ${track.actedAt.ifBlank { track.createTime.ifBlank { "-" } }}\n" +
                        track.actionRemark.ifBlank { "无审批意见" },
                ),
            )
        }
    }

    private fun renderVersions(detail: ConstructionLogDetail) {
        binding.versionContainer.removeAllViews()
        if (detail.versions.isEmpty()) {
            binding.versionContainer.addView(simpleText("暂无版本记录"))
            return
        }
        detail.versions.forEach { version ->
            binding.versionContainer.addView(
                simpleText(
                    "V${version.versionNo}  ${version.sourceAction.ifBlank { "-" }}\n" +
                        "${version.operatorName.ifBlank { "-" }}  ${version.createTime.ifBlank { "-" }}\n" +
                        version.diffSummary.ifBlank { "无差异摘要" },
                ),
            )
        }
    }

    private fun renderActions(detail: ConstructionLogDetail) {
        val actionCodes = detail.actionCodes.map { it.uppercase() }.toSet()
        val status = detail.status.uppercase()
        binding.editButton.isVisible = actionCodes.contains("EDIT") || status == "DRAFT" || status == "REJECTED"
        binding.deleteButton.isVisible = actionCodes.contains("DELETE") || status == "DRAFT" || status == "REJECTED"
        binding.submitButton.isVisible = actionCodes.contains("SUBMIT") || status == "DRAFT" || status == "REJECTED"
        binding.withdrawButton.isVisible = actionCodes.contains("WITHDRAW") || status == "SUBMITTED"
    }

    private fun submit(detail: ConstructionLogDetail) {
        lifecycleScope.launch {
            runCatching {
                val session = sessionStore.sessionFlow.first()
                repository.submit(session.accessToken, detail.projectId, detail.id, detail.version)
            }.onSuccess {
                toast("施工日志已提交审核")
                loadDetail()
            }.onFailure { throwable ->
                toast(throwable.message ?: "提交失败")
            }
        }
    }

    private fun withdraw(detail: ConstructionLogDetail) {
        lifecycleScope.launch {
            runCatching {
                val session = sessionStore.sessionFlow.first()
                repository.withdraw(session.accessToken, detail.projectId, detail.id, detail.version)
            }.onSuccess {
                toast("施工日志已撤回")
                loadDetail()
            }.onFailure { throwable ->
                toast(throwable.message ?: "撤回失败")
            }
        }
    }

    private fun delete(detail: ConstructionLogDetail) {
        lifecycleScope.launch {
            runCatching {
                val session = sessionStore.sessionFlow.first()
                repository.delete(session.accessToken, detail.projectId, detail.id, detail.version)
            }.onSuccess {
                toast("施工日志已删除")
                finish()
            }.onFailure { throwable ->
                toast(throwable.message ?: "删除失败")
            }
        }
    }

    private fun openEditor(logId: Long) {
        startActivity(
            Intent(this, ConstructionLogEditActivity::class.java)
                .putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, currentProjectId)
                .putExtra(ConstructionLogEditActivity.EXTRA_LOG_ID, logId),
        )
    }

    private fun simpleText(content: String): TextView {
        return TextView(this).apply {
            text = content
            setTextColor(getColor(R.color.text_secondary))
            textSize = 13f
            background = getDrawable(R.drawable.bg_search_pill)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            val params = androidx.appcompat.widget.LinearLayoutCompat.LayoutParams(
                androidx.appcompat.widget.LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                androidx.appcompat.widget.LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
            )
            params.topMargin = dp(8)
            layoutParams = params
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_LOG_ID = "logId"
    }
}
