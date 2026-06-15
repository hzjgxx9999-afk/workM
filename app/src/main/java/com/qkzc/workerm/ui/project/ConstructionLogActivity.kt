package com.qkzc.workerm.ui.project

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.data.project.ConstructionLogRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityConstructionLogBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConstructionLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConstructionLogBinding
    private lateinit var sessionStore: SessionStore
    private val repository = ConstructionLogRepository()
    private val adapter = ConstructionLogAdapter(::openDetail)
    private var currentProjectId: Long = 0L
    private var currentStatus: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityConstructionLogBinding.inflate(layoutInflater)
        sessionStore = SessionStore(applicationContext)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.constructionLogRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        binding.logRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.logRecyclerView.adapter = adapter
        binding.addButton.setOnClickListener { openEditor(null) }
        binding.createBottomButton.setOnClickListener { openEditor(null) }
        binding.refreshButton.setOnClickListener { loadData() }
        binding.searchButton.setOnClickListener { loadLogs() }
        binding.filterAllButton.setOnClickListener { updateFilter(null) }
        binding.filterDraftButton.setOnClickListener { updateFilter("DRAFT") }
        binding.filterPendingButton.setOnClickListener { updateFilter("SUBMITTED") }
        binding.filterApprovedButton.setOnClickListener { updateFilter("APPROVED") }
        currentProjectId = intent.getLongExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, 0L)
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun updateFilter(status: String?) {
        currentStatus = status
        binding.filterAllButton.isSelected = status == null
        binding.filterDraftButton.isSelected = status == "DRAFT"
        binding.filterPendingButton.isSelected = status == "SUBMITTED"
        binding.filterApprovedButton.isSelected = status == "APPROVED"
        loadLogs()
    }

    private fun loadData() {
        lifecycleScope.launch {
            runCatching {
                val session = sessionStore.sessionFlow.first()
                val projectId = currentProjectId.takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                currentProjectId = projectId
                val overview = repository.loadOverview(session.accessToken, projectId)
                val page = repository.loadPage(
                    token = session.accessToken,
                    projectId = projectId,
                    pageNum = 1,
                    pageSize = 50,
                    keyword = binding.searchInput.text?.toString().orEmpty(),
                    status = currentStatus,
                )
                overview to page.rows
            }.onSuccess { (overview, rows) ->
                binding.todayCountText.text = "今日记录\n${overview.todayCount} 条"
                binding.pendingCountText.text = "待审核\n${overview.pendingCount} 条"
                binding.archivedCountText.text = "本月归档\n${overview.archivedMonthCount} 条"
                binding.todayRecordText.text = if (rows.isEmpty()) {
                    "暂无日志记录"
                } else {
                    rows.first().toOverviewText()
                }
                adapter.submitList(rows)
                binding.emptyText.isVisible = rows.isEmpty()
                binding.logRecyclerView.isVisible = rows.isNotEmpty()
            }.onFailure { throwable ->
                binding.emptyText.isVisible = true
                binding.logRecyclerView.isVisible = false
                binding.emptyText.text = throwable.message ?: "施工日志加载失败"
                Toast.makeText(this@ConstructionLogActivity, throwable.message ?: "施工日志加载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadLogs() {
        lifecycleScope.launch {
            runCatching {
                val session = sessionStore.sessionFlow.first()
                val projectId = currentProjectId.takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                repository.loadPage(
                    token = session.accessToken,
                    projectId = projectId,
                    pageNum = 1,
                    pageSize = 50,
                    keyword = binding.searchInput.text?.toString().orEmpty(),
                    status = currentStatus,
                ).rows
            }.onSuccess { rows ->
                adapter.submitList(rows)
                binding.todayRecordText.text = if (rows.isEmpty()) "暂无日志记录" else rows.first().toOverviewText()
                binding.emptyText.isVisible = rows.isEmpty()
                binding.logRecyclerView.isVisible = rows.isNotEmpty()
            }.onFailure { throwable ->
                Toast.makeText(this@ConstructionLogActivity, throwable.message ?: "日志列表加载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openDetail(item: com.qkzc.workerm.data.project.ConstructionLogItem) {
        startActivity(
            Intent(this, ConstructionLogDetailActivity::class.java)
                .putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, currentProjectId)
                .putExtra(ConstructionLogDetailActivity.EXTRA_LOG_ID, item.id),
        )
    }

    private fun openEditor(logId: Long?) {
        startActivity(
            Intent(this, ConstructionLogEditActivity::class.java)
                .putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, currentProjectId)
                .apply {
                    if (logId != null) {
                        putExtra(ConstructionLogEditActivity.EXTRA_LOG_ID, logId)
                    }
                },
        )
    }

    private fun com.qkzc.workerm.data.project.ConstructionLogItem.toOverviewText(): String {
        return buildString {
            append("日期  ")
            append(logDate.ifBlank { "-" })
            append('\n')
            append("班组  ")
            append(teamName.ifBlank { "-" })
            append("    天气  见详情")
            append('\n')
            append("施工人数  ")
            append(workerCount)
            append(" 人    机械  ")
            append(machineCount)
            append(" 台")
            append('\n')
            append("完成内容  ")
            append(title.ifBlank { "无" })
        }
    }
}
