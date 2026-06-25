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
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.MainActivity
import com.qkzc.workerm.R
import com.qkzc.workerm.data.project.ManagerProjectTeam
import com.qkzc.workerm.data.project.ManagerProjectRepository
import com.qkzc.workerm.data.project.ProjectTeamRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.data.worker.ManagerWorker
import com.qkzc.workerm.data.worker.ManagerWorkerFilter
import com.qkzc.workerm.data.worker.ManagerWorkerPageMode
import com.qkzc.workerm.data.worker.ManagerWorkerRepository
import com.qkzc.workerm.data.worker.ProjectMemberPageState
import com.qkzc.workerm.data.worker.filterForMemberPage
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

    private lateinit var pageState: ProjectMemberPageState
    private var currentProjectId: Long = 0L
    private var currentFilter: ManagerWorkerFilter = ManagerWorkerFilter.ALL
    private var leaderFilterId: Long = 0L
    private var leaderFilterName: String = ""
    private var workers: List<ManagerWorker> = emptyList()
    private var teams: List<ManagerProjectTeam> = emptyList()
    private var hasLoaded = false

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
        pageState = ProjectMemberPageState.fromArgument(arguments?.getLong(ARG_PROJECT_ID, 0L) ?: 0L)
        currentProjectId = pageState.projectId ?: 0L
        leaderFilterId = arguments?.getLong(ARG_LEADER_ID, 0L) ?: 0L
        leaderFilterName = arguments?.getString(ARG_LEADER_NAME).orEmpty()

        binding.recyclerWorkers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerWorkers.adapter = workerAdapter
        binding.backButton.setOnClickListener { handleBack() }
        binding.buttonScan.setOnClickListener { openScan() }
        binding.buttonTeamManage.setOnClickListener { openTeamManage() }
        binding.buttonInviteCode.setOnClickListener { openInviteCodeOrScan() }
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
        configurePageChrome()
        bindFilterTabs()
        renderLoading()
        loadMembers()
    }

    private fun configurePageChrome() {
        binding.textTitle.text = pageState.title
        if (pageState.mode == ManagerWorkerPageMode.GLOBAL) {
            binding.buttonTeamManage.text = "班组长管理"
            binding.buttonInviteCode.text = "扫码查看"
        } else {
            binding.buttonTeamManage.text = "班组管理"
            binding.buttonInviteCode.text = "邀请码"
        }
    }

    private fun bindFilterTabs() {
        binding.tabAll.setOnClickListener { selectFilter(ManagerWorkerFilter.ALL) }
        binding.tabLeader.setOnClickListener { selectFilter(ManagerWorkerFilter.TEAM_LEADER) }
        binding.tabWorker.setOnClickListener { selectFilter(ManagerWorkerFilter.WORKER) }
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
                currentProjectId = pageState.projectId ?: 0L
                val keyword = binding.searchEditText.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
                val loadedWorkers = workerRepository.listWorkers(
                    token = session.accessToken,
                    projectId = pageState.workerQueryProjectId,
                    status = null,
                    keyword = keyword,
                ).filterByLeader()
                val loadedTeams = pageState.projectId
                    ?.let { teamRepository.loadTeams(session.accessToken, it) }
                    .orEmpty()
                loadedWorkers to loadedTeams
            }.onSuccess { (loadedWorkers, loadedTeams) ->
                hasLoaded = true
                workers = loadedWorkers
                teams = loadedTeams
                renderSummary()
                renderWorkers()
            }.onFailure { throwable ->
                hasLoaded = true
                workers = emptyList()
                teams = emptyList()
                renderSummary()
                renderError(throwable.message ?: "${pageState.title}加载失败")
            }
        }
    }

    private fun List<ManagerWorker>.filterByLeader(): List<ManagerWorker> {
        val leaderId = leaderFilterId.takeIf { it > 0L } ?: return this
        return filter { it.leaderId == leaderId || it.workerUserId == leaderId }
    }

    private fun renderLoading() {
        binding.textSummary.text = pageState.loadingText
        binding.textEmpty.isVisible = false
        binding.recyclerWorkers.isVisible = true
        workerAdapter.submitList(emptyList())
    }

    private fun renderSummary() {
        val summary = workers.toMemberSummary()
        val leaderCount = if (pageState.mode == ManagerWorkerPageMode.PROJECT) {
            teams.map { it.leaderId }.filter { it > 0L }.distinct().size
                .takeIf { it > 0 }
                ?: summary.teamLeaderCount
        } else {
            summary.teamLeaderCount
        }
        binding.textActiveStat.text = "在场工人\n${summary.activeWorkers}人"
        binding.textLeaderStat.text = "班组长\n${leaderCount}人"
        binding.textEnteringStat.text = "待入场\n${summary.enteringWorkers}人"
        binding.textAbnormalStat.text = "异常工人\n${summary.abnormalWorkers}人"
        val leaderPrefix = leaderFilterName.takeIf { it.isNotBlank() }?.let { "班组长：$it  " }.orEmpty()
        binding.textSummary.text = if (pageState.mode == ManagerWorkerPageMode.GLOBAL) {
            val projectCount = workers.map { it.projectId }.filter { it > 0L }.distinct().size
            "$leaderPrefix 共 ${summary.totalWorkers} 名工人，覆盖 ${projectCount} 个项目"
        } else {
            "$leaderPrefix 共 ${summary.totalWorkers} 名工人，${teams.size} 个班组"
        }
    }

    private fun renderWorkers() {
        val visibleWorkers = workers.filterForMemberPage(currentFilter)
        workerAdapter.submitList(visibleWorkers)
        binding.recyclerWorkers.isVisible = visibleWorkers.isNotEmpty()
        binding.textEmpty.isVisible = visibleWorkers.isEmpty()
        binding.textEmpty.text = if (workers.isEmpty()) pageState.emptyText else pageState.filterEmptyText
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
            ManagerWorkerFilter.TEAM_LEADER to binding.tabLeader,
            ManagerWorkerFilter.WORKER to binding.tabWorker,
            ManagerWorkerFilter.ENTERING to binding.tabEntering,
            ManagerWorkerFilter.EXITED to binding.tabExited,
        ).forEach { (filter, tab) ->
            val selected = filter == currentFilter
            tab.setTextColor(requireContext().getColor(if (selected) R.color.dashboard_blue else R.color.text_secondary))
            tab.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun openWorkerDetail(worker: ManagerWorker) {
        val workerUserId = worker.workerUserId.takeIf { it > 0L } ?: return
        val projectId = worker.projectId.takeIf { it > 0L } ?: currentProjectId.takeIf { it > 0L }
        if (projectId == null) {
            Toast.makeText(requireContext(), "缺少工人所属项目，无法查看详情", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(requireContext(), WorkerDetailActivity::class.java)
                .putExtra(WorkerDetailActivity.EXTRA_PROJECT_ID, projectId)
                .putExtra(WorkerDetailActivity.EXTRA_WORKER_ID, workerUserId),
        )
    }

    private fun openScan() {
        if (pageState.mode == ManagerWorkerPageMode.PROJECT) {
            openProjectActivity(WorkerScanActivity::class.java)
        } else {
            openGlobalScanWithProjectSelection()
        }
    }

    private fun openTeamManage() {
        if (pageState.mode == ManagerWorkerPageMode.PROJECT) {
            openProjectActivity(ProjectTeamManageActivity::class.java)
        } else {
            startActivity(Intent(requireContext(), TeamLeaderActivity::class.java))
        }
    }

    private fun openInviteCodeOrScan() {
        if (pageState.mode == ManagerWorkerPageMode.PROJECT) {
            openProjectActivity(InviteCodeManageActivity::class.java)
        } else {
            openGlobalScanWithProjectSelection()
        }
    }

    private fun openGlobalScanWithProjectSelection() {
        lifecycleScope.launch {
            runCatching {
                val session = SessionStore(requireContext()).sessionFlow.first()
                projectRepository.loadProjects(session.accessToken)
            }.onSuccess { projects ->
                when (projects.size) {
                    0 -> Toast.makeText(requireContext(), "当前账号没有可扫码核验的项目", Toast.LENGTH_SHORT).show()
                    1 -> startActivity(
                        Intent(requireContext(), WorkerScanActivity::class.java)
                            .putExtra(WorkerScanActivity.EXTRA_PROJECT_ID, projects.first().projectId),
                    )
                    else -> AlertDialog.Builder(requireContext())
                        .setTitle("选择扫码核验项目")
                        .setItems(projects.map { it.projectName.ifBlank { "项目 ${it.projectId}" } }.toTypedArray()) { dialog, which ->
                            projects.getOrNull(which)?.let { project ->
                                startActivity(
                                    Intent(requireContext(), WorkerScanActivity::class.java)
                                        .putExtra(WorkerScanActivity.EXTRA_PROJECT_ID, project.projectId),
                                )
                            }
                            dialog.dismiss()
                        }
                        .show()
                }
            }.onFailure {
                Toast.makeText(requireContext(), it.message ?: "项目加载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun <T> openProjectActivity(activityClass: Class<T>) {
        if (currentProjectId <= 0L) {
            Toast.makeText(requireContext(), "缺少项目 ID", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(requireContext(), activityClass)
        intent.putExtra(ProjectTeamManageActivity.EXTRA_PROJECT_ID, currentProjectId)
        intent.putExtra(InviteCodeManageActivity.EXTRA_PROJECT_ID, currentProjectId)
        intent.putExtra(WorkerScanActivity.EXTRA_PROJECT_ID, currentProjectId)
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
        if (hasLoaded) {
            loadMembers()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PROJECT_ID = "projectId"
        private const val ARG_LEADER_ID = "leaderId"
        private const val ARG_LEADER_NAME = "leaderName"

        fun newGlobalInstance(leaderId: Long = 0L, leaderName: String = ""): ProjectMemberManageFragment {
            return newInstance(projectId = null, leaderId = leaderId, leaderName = leaderName)
        }

        fun newInstance(
            projectId: Long? = null,
            leaderId: Long = 0L,
            leaderName: String = "",
        ): ProjectMemberManageFragment {
            return ProjectMemberManageFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PROJECT_ID, projectId ?: 0L)
                    putLong(ARG_LEADER_ID, leaderId)
                    putString(ARG_LEADER_NAME, leaderName)
                }
            }
        }
    }
}
