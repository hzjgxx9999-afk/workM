package com.qkzc.workerm.ui.invite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.data.invite.InviteCodeRepository
import com.qkzc.workerm.data.invite.ManageInviteCode
import com.qkzc.workerm.data.project.ManagerProject
import com.qkzc.workerm.data.project.ManagerProjectRepository
import com.qkzc.workerm.data.project.ManagerProjectTeam
import com.qkzc.workerm.data.project.ProjectTeamRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.FragmentInviteCodeManageBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class InviteCodeManageFragment : Fragment() {

    private var _binding: FragmentInviteCodeManageBinding? = null
    private val binding: FragmentInviteCodeManageBinding
        get() = checkNotNull(_binding)

    private val projectRepository = ManagerProjectRepository()
    private val projectTeamRepository = ProjectTeamRepository()
    private val inviteCodeRepository = InviteCodeRepository()
    private lateinit var sessionStore: SessionStore
    private val inviteAdapter = InviteCodeAdapter(
        onCopyClick = ::copyInvite,
        onShareClick = ::shareInvite,
        onStatusClick = ::toggleStatus,
    )

    private var projects: List<ManagerProject> = emptyList()
    private var teams: List<ManagerProjectTeam> = emptyList()
    private var selectedProject: ManagerProject? = null
    private var selectedTeam: ManagerProjectTeam? = null
    private val lockedProjectId: Long?
        get() = arguments?.getLong(ARG_PROJECT_ID, 0L)?.takeIf { it > 0L }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInviteCodeManageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionStore = SessionStore(requireContext().applicationContext)
        binding.recyclerInviteCodes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerInviteCodes.adapter = inviteAdapter
        binding.backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.buttonRefresh.setOnClickListener { reloadCurrentProject() }
        binding.buttonCreateInvite.setOnClickListener { createInviteCode() }
        binding.inputExpireTime.setText(defaultExpireTime())
        setupSpinners()
        loadProjects()
    }

    private fun setupSpinners() {
        binding.spinnerProject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedProject = projects.getOrNull(position)
                selectedTeam = null
                loadLeadersAndInvites()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.spinnerTeam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedTeam = teams.getOrNull(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun loadProjects() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val allProjects = projectRepository.loadProjects(currentToken())
                lockedProjectId?.let { projectId ->
                    allProjects.filter { it.projectId == projectId }
                } ?: allProjects
            }.onSuccess { result ->
                projects = result
                selectedProject = projects.firstOrNull()
                bindProjectSpinner()
                binding.spinnerProject.isEnabled = lockedProjectId == null
                if (projects.isEmpty()) {
                    renderInvites(emptyList())
                    toast("暂无可管理项目")
                }
            }.onFailure { throwable ->
                renderInvites(emptyList())
                toast(throwable.message ?: "项目加载失败")
            }
        }
    }

    private fun bindProjectSpinner() {
        binding.spinnerProject.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            projects.map { it.projectName.ifBlank { "项目 ${it.projectId}" } },
        )
    }

    private fun loadLeadersAndInvites() {
        val projectId = selectedProject?.projectId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val token = currentToken()
                val projectTeams = projectTeamRepository.loadTeams(token, projectId)
                val inviteCodes = inviteCodeRepository.loadList(token, projectId = projectId)
                projectTeams to inviteCodes
            }.onSuccess { (projectTeams, inviteCodes) ->
                teams = projectTeams.filter { it.enabled }
                selectedTeam = teams.firstOrNull()
                bindTeamSpinner()
                renderInvites(inviteCodes)
            }.onFailure { throwable ->
                teams = emptyList()
                bindTeamSpinner()
                renderInvites(emptyList())
                toast(throwable.message ?: "邀请码加载失败")
            }
        }
    }

    private fun bindTeamSpinner() {
        val names = if (teams.isEmpty()) {
            listOf("暂无可用班组")
        } else {
            teams.map {
                val leader = it.leaderName.ifBlank { "班组长 ${it.leaderId}" }
                "${it.teamName.ifBlank { "班组 ${it.teamId}" }} / $leader"
            }
        }
        binding.spinnerTeam.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            names,
        )
    }

    private fun reloadCurrentProject() {
        if (selectedProject == null) {
            loadProjects()
        } else {
            loadLeadersAndInvites()
        }
    }

    private fun createInviteCode() {
        val project = selectedProject ?: return toast("请选择项目")
        val team = selectedTeam ?: return toast("请选择班组")
        if (team.teamId <= 0L || team.leaderId <= 0L) {
            toast("班组信息不完整，请先维护班组")
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            binding.buttonCreateInvite.isEnabled = false
            runCatching {
                inviteCodeRepository.create(
                    token = currentToken(),
                    projectId = project.projectId,
                    leaderId = team.leaderId,
                    teamId = team.teamId,
                    maxUseCount = binding.inputMaxUseCount.text?.toString()?.toIntOrNull(),
                    expireTime = binding.inputExpireTime.text?.toString()?.trim().orEmpty(),
                    remark = binding.inputRemark.text?.toString()?.trim().orEmpty(),
                )
            }.onSuccess { created ->
                toast("邀请码已生成：${created.inviteCode}")
                binding.inputRemark.text?.clear()
                loadLeadersAndInvites()
            }.onFailure { throwable ->
                toast(throwable.message ?: "邀请码生成失败")
            }
            binding.buttonCreateInvite.isEnabled = true
        }
    }

    private fun renderInvites(invites: List<ManageInviteCode>) {
        inviteAdapter.submitList(invites)
        binding.recyclerInviteCodes.isVisible = invites.isNotEmpty()
        binding.textEmpty.isVisible = invites.isEmpty()
        val enabledCount = invites.count { it.enabled }
        binding.textInviteSummary.text = "共 ${invites.size} 个邀请码，启用 $enabledCount 个"
    }

    private fun toggleStatus(item: ManageInviteCode) {
        val target = if (item.enabled) "DISABLED" else "ENABLED"
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                inviteCodeRepository.updateStatus(currentToken(), item.id, target)
            }.onSuccess {
                toast(if (target == "ENABLED") "已启用" else "已停用")
                loadLeadersAndInvites()
            }.onFailure { throwable ->
                toast(throwable.message ?: "状态更新失败")
            }
        }
    }

    private fun copyInvite(item: ManageInviteCode) {
        val manager = requireContext().getSystemService(ClipboardManager::class.java)
        manager.setPrimaryClip(ClipData.newPlainText("invite-code", item.qrContent))
        toast("已复制注册链接")
    }

    private fun shareInvite(item: ManageInviteCode) {
        val text = "工人注册邀请码：${item.inviteCode}\n${item.qrContent}"
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, text),
                "分享邀请码",
            ),
        )
    }

    private suspend fun currentToken(): String {
        return sessionStore.sessionFlow.first().accessToken.takeIf { it.isNotBlank() }
            ?: error("请先登录")
    }

    private fun defaultExpireTime(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return LocalDateTime.now().plusDays(30).withHour(23).withMinute(59).withSecond(59).format(formatter)
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerInviteCodes.adapter = null
        _binding = null
    }

    companion object {
        private const val ARG_PROJECT_ID = "projectId"

        fun newInstance(projectId: Long? = null): InviteCodeManageFragment {
            return InviteCodeManageFragment().apply {
                if (projectId != null && projectId > 0L) {
                    arguments = Bundle().apply { putLong(ARG_PROJECT_ID, projectId) }
                }
            }
        }
    }
}
