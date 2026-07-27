package com.qkzc.workerm.ui.dispatch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.data.dispatch.DispatchRepository
import com.qkzc.workerm.data.project.ManagerProject
import com.qkzc.workerm.data.project.ManagerProjectRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityDispatchListBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DispatchListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDispatchListBinding
    private val repository = DispatchRepository()
    private val projectRepository = ManagerProjectRepository()
    private val sessionStore by lazy { SessionStore(applicationContext) }
    private val adapter = ManagerDispatchAdapter {
        startActivity(DispatchDetailActivity.intent(this, it.id))
    }
    private val statuses: List<String?> = listOf(
        null,
        "PENDING_DELEGATION",
        "IN_PROGRESS",
        "PENDING_MANAGER_ACCEPTANCE",
        "COMPLETED",
    )

    private var projects: List<ManagerProject> = emptyList()
    private var selectedProjectId: Long? = null
    private var selectedProjectName: String = ""
    private var projectReady: Boolean = false
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDispatchListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets()

        selectedProjectId = intent.getLongExtra(EXTRA_PROJECT_ID, 0L).takeIf { it > 0L }
        selectedProjectName = intent.getStringExtra(EXTRA_PROJECT_NAME).orEmpty()

        binding.backButton.setOnClickListener { finish() }
        binding.createFab.setOnClickListener { openCreateDispatch() }
        binding.projectFilterButton.setOnClickListener { showProjectSelector() }
        binding.dispatchRecycler.layoutManager = LinearLayoutManager(this)
        binding.dispatchRecycler.adapter = adapter
        binding.statusSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("全部", "待分派", "处理中", "待验收", "已完成"),
        )
        binding.statusSpinner.setSelection(0, false)
        binding.statusSpinner.onItemSelectedListener = SimpleItemSelectedListener {
            if (projectReady) load(currentStatus())
        }
        updateProjectHeader()
        loadProjectOptions()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized && projectReady) load(currentStatus())
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.createFab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = dp(24) + bars.bottom
            }
            binding.dispatchRecycler.setPadding(
                binding.dispatchRecycler.paddingLeft,
                binding.dispatchRecycler.paddingTop,
                binding.dispatchRecycler.paddingRight,
                dp(96) + bars.bottom,
            )
            insets
        }
    }

    private fun loadProjectOptions() {
        lifecycleScope.launch {
            runCatching {
                val session = sessionStore.sessionFlow.first()
                val preferredProjectId = selectedProjectId ?: session.projectId.toLongOrNull()
                val token = session.accessToken.takeIf { it.isNotBlank() } ?: error("请先登录")
                ProjectOptions(
                    preferredProjectId = preferredProjectId,
                    projects = projectRepository.loadProjects(token),
                )
            }.onSuccess { options ->
                projects = options.projects
                val selected = projects.firstOrNull { it.projectId == options.preferredProjectId }
                    ?: projects.firstOrNull()
                if (selected != null) {
                    selectProject(selected, reload = false)
                    projectReady = true
                    load(currentStatus())
                } else if (options.preferredProjectId != null) {
                    selectedProjectId = options.preferredProjectId
                    projectReady = true
                    updateProjectHeader()
                    load(currentStatus())
                } else {
                    selectedProjectId = null
                    selectedProjectName = ""
                    projectReady = true
                    updateProjectHeader()
                    showEmpty("暂无可管理项目")
                }
            }.onFailure { throwable ->
                projectReady = true
                updateProjectHeader()
                val projectId = selectedProjectId
                if (projectId == null) {
                    showEmpty(throwable.message ?: "项目加载失败")
                } else {
                    load(currentStatus())
                }
            }
        }
    }

    private fun showProjectSelector() {
        if (projects.isEmpty()) {
            toast("暂无可切换项目")
            return
        }
        val checked = projects.indexOfFirst { it.projectId == selectedProjectId }
        AlertDialog.Builder(this)
            .setTitle("切换项目")
            .setSingleChoiceItems(projects.map { it.projectName.ifBlank { "未命名项目" } }.toTypedArray(), checked) { dialog, which ->
                projects.getOrNull(which)?.let { selectProject(it, reload = true) }
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun selectProject(project: ManagerProject, reload: Boolean) {
        selectedProjectId = project.projectId.takeIf { it > 0L }
        selectedProjectName = project.projectName
        updateProjectHeader()
        if (reload) load(currentStatus())
    }

    private fun updateProjectHeader() {
        val name = selectedProjectName.ifBlank {
            selectedProjectId?.let { "项目 $it" }.orEmpty()
        }
        binding.projectFilterButton.text = name.ifBlank { "选择项目" }
        binding.titleText.text = if (name.isBlank()) "派工单" else "$name · 派工单"
    }

    private fun openCreateDispatch() {
        val projectId = selectedProjectId?.takeIf { it > 0L }
        if (projectId == null) {
            toast("请先选择项目")
            return
        }
        startActivity(
            Intent(this, DispatchCreateActivity::class.java)
                .putExtra(DispatchCreateActivity.EXTRA_PROJECT_ID, projectId),
        )
    }

    private fun currentStatus(): String? {
        return statuses.getOrNull(binding.statusSpinner.selectedItemPosition)
    }

    private fun load(status: String?): Unit {
        val projectId = selectedProjectId?.takeIf { it > 0L }
        if (projectId == null) {
            loadJob?.cancel()
            adapter.submitList(emptyList())
            showEmpty("请先选择项目")
            return
        }
        loadJob?.cancel()
        adapter.submitList(emptyList())
        binding.emptyText.visibility = View.GONE
        loadJob = lifecycleScope.launch {
            runCatching {
                val token = sessionStore.sessionFlow.first().accessToken
                repository.page(
                    token = token,
                    status = status,
                    projectId = projectId,
                )
            }.onSuccess {
                if (selectedProjectId != projectId) return@onSuccess
                adapter.submitList(it)
                binding.emptyText.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                binding.emptyText.text = "暂无派工任务"
                binding.emptyText.setOnClickListener(null)
            }.onFailure {
                if (it is CancellationException) return@onFailure
                if (selectedProjectId != projectId) return@onFailure
                adapter.submitList(emptyList())
                binding.emptyText.text = "加载失败，点击重试"
                binding.emptyText.visibility = View.VISIBLE
                binding.emptyText.setOnClickListener { load(status) }
            }
        }
    }

    private fun showEmpty(message: String) {
        adapter.submitList(emptyList())
        binding.emptyText.text = message
        binding.emptyText.visibility = View.VISIBLE
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class ProjectOptions(
        val preferredProjectId: Long?,
        val projects: List<ManagerProject>,
    )

    companion object {
        const val EXTRA_PROJECT_ID = "projectId"
        const val EXTRA_PROJECT_NAME = "projectName"

        fun intent(context: Context, projectId: Long? = null, projectName: String? = null): Intent {
            return Intent(context, DispatchListActivity::class.java).apply {
                projectId?.takeIf { it > 0L }?.let { putExtra(EXTRA_PROJECT_ID, it) }
                projectName?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_PROJECT_NAME, it) }
            }
        }
    }
}
