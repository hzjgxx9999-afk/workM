package com.qkzc.workerm.ui.worker

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.data.project.ManagerProjectRepository
import com.qkzc.workerm.data.project.ManagerProjectTeam
import com.qkzc.workerm.data.project.ProjectTeamRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.data.worker.ManagerTeamLeaderScope
import com.qkzc.workerm.data.worker.ManagerTeamLeaderSummary
import com.qkzc.workerm.data.worker.ManagerWorker
import com.qkzc.workerm.data.worker.ManagerWorkerRepository
import com.qkzc.workerm.data.worker.toTeamLeaderSummaries
import com.qkzc.workerm.databinding.ActivityTeamLeaderBinding
import com.qkzc.workerm.ui.dispatch.DispatchCreateActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TeamLeaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeamLeaderBinding
    private val projectRepository = ManagerProjectRepository()
    private val teamRepository = ProjectTeamRepository()
    private val workerRepository = ManagerWorkerRepository()
    private val adapter = ManagerTeamLeaderAdapter(
        onMembersClick = ::openLeaderMembers,
        onDispatchClick = ::openDispatchForLeader,
    )

    private var summaries: List<ManagerTeamLeaderSummary> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTeamLeaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.teamRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        binding.createDispatchButton.setOnClickListener {
            startActivity(Intent(this, DispatchCreateActivity::class.java))
        }
        binding.leaderRefreshButton.setOnClickListener { loadLeaders() }
        binding.leaderSearchText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                renderLeaders()
                true
            } else {
                false
            }
        }
        binding.leaderRecycler.layoutManager = LinearLayoutManager(this)
        binding.leaderRecycler.adapter = adapter
        loadLeaders()
    }

    private fun loadLeaders() {
        lifecycleScope.launch {
            renderLoading()
            runCatching {
                val token = SessionStore(applicationContext).sessionFlow.first().accessToken
                val projects = projectRepository.loadProjects(token)
                val teamsByProject = mutableMapOf<Long, List<ManagerProjectTeam>>()
                val workersByProject = mutableMapOf<Long, List<ManagerWorker>>()
                projects.forEach { project ->
                    val projectId = project.projectId
                    teamsByProject[projectId] = teamRepository.loadTeams(token, projectId)
                    workersByProject[projectId] = workerRepository.listWorkers(token, projectId)
                }
                toTeamLeaderSummaries(projects, teamsByProject, workersByProject)
            }.onSuccess {
                summaries = it
                renderLeaders()
            }.onFailure {
                summaries = emptyList()
                renderError(it.message ?: "班组长加载失败")
            }
        }
    }

    private fun renderLoading() {
        binding.leaderSummaryText.text = "正在加载班组长..."
        binding.leaderEmptyText.isVisible = false
        binding.leaderRecycler.isVisible = true
        adapter.submitList(emptyList())
    }

    private fun renderLeaders() {
        val keyword = binding.leaderSearchText.text?.toString()?.trim().orEmpty()
        val visible = summaries.filter { summary ->
            keyword.isBlank() ||
                summary.leaderName.contains(keyword, ignoreCase = true) ||
                summary.mobile.contains(keyword)
        }
        adapter.submitList(visible)
        binding.leaderRecycler.isVisible = visible.isNotEmpty()
        binding.leaderEmptyText.isVisible = visible.isEmpty()
        binding.leaderEmptyText.text = if (summaries.isEmpty()) "暂无班组长" else "当前搜索下暂无班组长"
        val workerCount = summaries.sumOf { it.workerCount }
        val projectCount = summaries.flatMap { it.scopes.map(ManagerTeamLeaderScope::projectId) }.distinct().size
        binding.leaderSummaryText.text = "共 ${summaries.size} 名班组长，覆盖 ${projectCount} 个项目，管理 ${workerCount} 名成员"
    }

    private fun renderError(message: String) {
        adapter.submitList(emptyList())
        binding.leaderRecycler.isVisible = false
        binding.leaderEmptyText.isVisible = true
        binding.leaderEmptyText.text = message
        binding.leaderSummaryText.text = "加载失败"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun openLeaderMembers(summary: ManagerTeamLeaderSummary) {
        startActivity(
            Intent(this, ProjectMemberManageActivity::class.java)
                .putExtra(ProjectMemberManageActivity.EXTRA_LEADER_ID, summary.leaderId)
                .putExtra(ProjectMemberManageActivity.EXTRA_LEADER_NAME, summary.leaderName),
        )
    }

    private fun openDispatchForLeader(summary: ManagerTeamLeaderSummary) {
        when (summary.scopes.size) {
            0 -> Toast.makeText(this, "该班组长暂无可派遣项目", Toast.LENGTH_SHORT).show()
            1 -> openDispatch(summary.scopes.first())
            else -> AlertDialog.Builder(this)
                .setTitle("选择派遣项目")
                .setItems(summary.scopes.map { "${it.projectName.ifBlank { "项目 ${it.projectId}" }} · ${it.teamName.ifBlank { "班组 ${it.teamId}" }}" }.toTypedArray()) { dialog, which ->
                    summary.scopes.getOrNull(which)?.let(::openDispatch)
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun openDispatch(scope: ManagerTeamLeaderScope) {
        startActivity(
            Intent(this, DispatchCreateActivity::class.java)
                .putExtra(DispatchCreateActivity.EXTRA_PROJECT_ID, scope.projectId)
                .putExtra(DispatchCreateActivity.EXTRA_TEAM_ID, scope.teamId)
                .putExtra(DispatchCreateActivity.EXTRA_LEADER_ID, scope.leaderId),
        )
    }
}
