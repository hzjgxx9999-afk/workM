package com.qkzc.workerm.ui.worker

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.MainActivity
import com.qkzc.workerm.R
import com.qkzc.workerm.data.project.ManagerProjectTeam
import com.qkzc.workerm.data.project.ProjectTeamRepository
import com.qkzc.workerm.data.project.ManagerProjectRepository
import com.qkzc.workerm.data.session.LoginSession
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.data.worker.ManagerWorker
import com.qkzc.workerm.data.worker.ManagerWorkerFilter
import com.qkzc.workerm.data.worker.ManagerWorkerRepository
import com.qkzc.workerm.data.worker.normalizedBindStatus
import com.qkzc.workerm.data.worker.toMemberSummary
import com.qkzc.workerm.databinding.FragmentProjectMemberManageBinding
import com.qkzc.workerm.ui.invite.InviteCodeManageActivity
import com.qkzc.workerm.ui.project.ProjectTeamManageActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProjectMemberManageFragment : Fragment() {

    private var _binding: FragmentProjectMemberManageBinding? = null
    private val binding: FragmentProjectMemberManageBinding
        get() = checkNotNull(_binding)

    private val workerRepository = ManagerWorkerRepository()
    private val projectRepository = ManagerProjectRepository()
    private val teamRepository = ProjectTeamRepository()
    private val workerAdapter = ManagerWorkerAdapter { openWorkerDetail(it) }

    private var currentProjectId: Long = 0L
    private var currentFilter: ManagerWorkerFilter = ManagerWorkerFilter.ALL
    private var workers: List<ManagerWorker> = emptyList()
    private var teams: List<ManagerProjectTeam> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProjectMemberManageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerWorkers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerWorkers.adapter = workerAdapter
        binding.backButton.setOnClickListener { handleBack() }
        binding.buttonScan.setOnClickListener { openProjectActivity(WorkerScanActivity::class.java) }
        binding.buttonTeamManage.setOnClickListener { openProjectActivity(ProjectTeamManageActivity::class.java) }
        binding.buttonInviteCode.setOnClickListener { openProjectActivity(InviteCodeManageActivity::class.java) }
        binding.buttonRefresh.setOnClickListener { loadMembers() }
        binding.buttonSearch.setOnClickListener { loadMembers() }
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadMembers()
                true
            } else {
                false
            }
        }
        bindFilterTabs()
        renderLoading()
        loadMembers()
    }

    private fun bindFilterTabs() {
        binding.tabAll.setOnClickListener { selectFilter(ManagerWorkerFilter.ALL) }
        binding.tabActive.setOnClickListener { selectFilter(ManagerWorkerFilter.ACTIVE) }
        binding.tabEntering.setOnClickListener { selectFilter(ManagerWorkerFilter.ENTERING) }
        binding.tabExited.setOnClickListener { selectFilter(ManagerWorkerFilter.EXITED) }
        renderFilterTabs()
    }

    private fun selectFilter(filter: ManagerWorkerFilter) {
        currentFilter = filter
        renderFilterTabs()
        renderWorkers()
    }

    private fun loadMembers() {
        lifecycleScope.launch {
            renderLoading()
            runCatching {
                val session = SessionStore(requireContext()).sessionFlow.first()
                currentProjectId = resolveProjectId(session)
                val keyword = binding.searchEditText.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
                val loadedWorkers = workerRepository.listWorkers(
                    token = session.accessToken,
                    projectId = currentProjectId,
                    status = null,
                    keyword = keyword,
                )
                val loadedTeams = teamRepository.loadTeams(session.accessToken, currentProjectId)
                loadedWorkers to loadedTeams
            }.onSuccess { (loadedWorkers, loadedTeams) ->
                workers = loadedWorkers
                teams = loadedTeams
                renderSummary()
                renderWorkers()
            }.onFailure { throwable ->
                workers = emptyList()
                teams = emptyList()
                renderSummary()
                renderError(throwable.message ?: "项目成员加载失败")
            }
        }
    }

    private suspend fun resolveProjectId(session: LoginSession): Long {
        arguments?.getLong(ARG_PROJECT_ID, 0L)?.takeIf { it > 0L }?.let { return it }
        session.projectId.toLongOrNull()?.takeIf { it > 0L }?.let { return it }
        return projectRepository.loadProjects(session.accessToken).firstOrNull()?.projectId
            ?: error("当前账号没有可管理项目")
    }

    private fun renderLoading() {
        binding.textSummary.text = "正在加载项目成员..."
        binding.textEmpty.isVisible = false
        binding.recyclerWorkers.isVisible = true
        workerAdapter.submitList(emptyList())
    }

    private fun renderSummary() {
        val summary = workers.toMemberSummary()
        val leaderCount = teams.map { it.leaderId }.filter { it > 0L }.distinct().size
            .takeIf { it > 0 }
            ?: summary.teamLeaderCount
        binding.textActiveStat.text = "在场工人\n${summary.activeWorkers}人"
        binding.textLeaderStat.text = "班组长\n${leaderCount}人"
        binding.textEnteringStat.text = "待入场\n${summary.enteringWorkers}人"
        binding.textAbnormalStat.text = "异常工人\n${summary.abnormalWorkers}人"
        binding.textSummary.text = "共 ${summary.totalWorkers} 名工人，${teams.size} 个班组"
    }

    private fun renderWorkers() {
        val visibleWorkers = workers.filterBy(currentFilter)
        workerAdapter.submitList(visibleWorkers)
        binding.recyclerWorkers.isVisible = visibleWorkers.isNotEmpty()
        binding.textEmpty.isVisible = visibleWorkers.isEmpty()
        binding.textEmpty.text = if (workers.isEmpty()) "暂无项目成员" else "当前筛选下暂无成员"
    }

    private fun renderError(message: String) {
        workerAdapter.submitList(emptyList())
        binding.recyclerWorkers.isVisible = false
        binding.textEmpty.isVisible = true
        binding.textEmpty.text = message
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun renderFilterTabs() {
        mapOf(
            ManagerWorkerFilter.ALL to binding.tabAll,
            ManagerWorkerFilter.ACTIVE to binding.tabActive,
            ManagerWorkerFilter.ENTERING to binding.tabEntering,
            ManagerWorkerFilter.EXITED to binding.tabExited,
        ).forEach { (filter, tab) ->
            val selected = filter == currentFilter
            tab.setTextColor(requireContext().getColor(if (selected) R.color.dashboard_blue else R.color.text_secondary))
            tab.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun List<ManagerWorker>.filterBy(filter: ManagerWorkerFilter): List<ManagerWorker> {
        return when (filter) {
            ManagerWorkerFilter.ALL -> this
            ManagerWorkerFilter.ACTIVE -> filter { it.normalizedBindStatus == "ACTIVE" }
            ManagerWorkerFilter.ENTERING -> filter {
                it.normalizedBindStatus == "ENTERING" || it.normalizedBindStatus == "BOUND"
            }
            ManagerWorkerFilter.EXITED -> filter {
                it.normalizedBindStatus == "EXITED" || it.normalizedBindStatus == "LEFT"
            }
        }
    }

    private fun openWorkerDetail(worker: ManagerWorker) {
        val workerUserId = worker.workerUserId.takeIf { it > 0L } ?: return
        startActivity(
            Intent(requireContext(), WorkerDetailActivity::class.java)
                .putExtra(WorkerDetailActivity.EXTRA_PROJECT_ID, currentProjectId.takeIf { it > 0L } ?: worker.projectId)
                .putExtra(WorkerDetailActivity.EXTRA_WORKER_ID, workerUserId),
        )
    }

    private fun <T> openProjectActivity(activityClass: Class<T>) {
        val intent = Intent(requireContext(), activityClass)
        if (currentProjectId > 0L) {
            intent.putExtra(ProjectTeamManageActivity.EXTRA_PROJECT_ID, currentProjectId)
            intent.putExtra(InviteCodeManageActivity.EXTRA_PROJECT_ID, currentProjectId)
            intent.putExtra(WorkerScanActivity.EXTRA_PROJECT_ID, currentProjectId)
        }
        startActivity(intent)
    }

    private fun handleBack() {
        if (activity is ProjectMemberManageActivity) {
            requireActivity().finish()
        } else {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_home)
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentProjectId > 0L) {
            loadMembers()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PROJECT_ID = "projectId"

        fun newInstance(projectId: Long? = null): ProjectMemberManageFragment {
            return ProjectMemberManageFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PROJECT_ID, projectId ?: 0L)
                }
            }
        }
    }
}
