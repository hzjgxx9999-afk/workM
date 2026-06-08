package com.qkzc.workerm.ui.project

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.data.project.ManagerLeaderOption
import com.qkzc.workerm.data.project.ManagerProjectTeam
import com.qkzc.workerm.data.project.ManagerWorkTypeOption
import com.qkzc.workerm.data.project.ProjectTeamRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.FragmentProjectTeamManageBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProjectTeamManageFragment : Fragment() {

    private var _binding: FragmentProjectTeamManageBinding? = null
    private val binding: FragmentProjectTeamManageBinding
        get() = checkNotNull(_binding)

    private val repository = ProjectTeamRepository()
    private lateinit var sessionStore: SessionStore
    private val teamAdapter = ProjectTeamAdapter(
        onEditClick = ::showTeamDialog,
        onStatusClick = ::toggleStatus,
    )
    private val projectId: Long
        get() = arguments?.getLong(ARG_PROJECT_ID, 0L)?.takeIf { it > 0L } ?: 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProjectTeamManageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionStore = SessionStore(requireContext().applicationContext)
        binding.recyclerTeams.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTeams.adapter = teamAdapter
        binding.backButton.setOnClickListener { requireActivity().finish() }
        binding.buttonAddTeam.setOnClickListener { showTeamDialog(null) }
        loadTeams()
    }

    private fun loadTeams() {
        if (projectId <= 0L) {
            toast("缺少项目 ID")
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.loadTeams(currentToken(), projectId)
            }.onSuccess { teams ->
                teamAdapter.submitList(teams)
                binding.recyclerTeams.isVisible = teams.isNotEmpty()
                binding.textEmpty.isVisible = teams.isEmpty()
                val enabledCount = teams.count { it.enabled }
                binding.textSummary.text = "共 ${teams.size} 个班组，启用 $enabledCount 个"
            }.onFailure { throwable ->
                teamAdapter.submitList(emptyList())
                binding.recyclerTeams.isVisible = false
                binding.textEmpty.isVisible = true
                binding.textSummary.text = "加载失败"
                toast(throwable.message ?: "班组加载失败")
            }
        }
    }

    private fun showTeamDialog(team: ManagerProjectTeam?) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val token = currentToken()
                TeamDialogOptions(
                    token = token,
                    leaders = repository.loadLeaderOptions(token, projectId),
                    workTypes = repository.loadWorkTypeOptions(token, projectId),
                )
            }.onSuccess { options ->
                showTeamDialogWithOptions(
                    team = team,
                    token = options.token,
                    leaders = options.leaders.withCurrentLeader(team),
                    workTypes = options.workTypes.withCurrentWorkType(team),
                )
            }.onFailure { throwable ->
                toast(throwable.message ?: "选项加载失败")
            }
        }
    }

    private fun showTeamDialogWithOptions(
        team: ManagerProjectTeam?,
        token: String,
        leaders: List<ManagerLeaderOption>,
        workTypes: List<ManagerWorkTypeOption>,
    ) {
        if (leaders.isEmpty()) {
            toast("暂无可选班组长")
            return
        }
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 20, 48, 0)
        }
        val teamNameInput = EditText(context).apply {
            hint = "班组名称"
            setText(team?.teamName.orEmpty())
            maxLines = 1
        }
        var currentLeaders = leaders
        var currentWorkTypes = workTypes
        var selectedLeader: ManagerLeaderOption? = currentLeaders.firstOrNull { it.leaderId == team?.leaderId }
        var selectedWorkType: ManagerWorkTypeOption? = currentWorkTypes.firstOrNull { it.workTypeId == team?.workTypeId }
        val leaderAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            currentLeaders.map { it.displayText() }.toMutableList(),
        )
        val workTypeAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            currentWorkTypes.map { it.displayText() }.toMutableList(),
        )
        val leaderInput = AutoCompleteTextView(context).apply {
            hint = "输入姓名或手机号搜索"
            threshold = 0
            setSingleLine(true)
            setAdapter(leaderAdapter)
            setText(selectedLeader?.displayText().orEmpty(), false)
            setOnClickListener { showDropDown() }
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDropDown() }
            setOnItemClickListener { _, _, position, _ ->
                selectedLeader = currentLeaders.getOrNull(position)
            }
        }
        val workTypeInput = AutoCompleteTextView(context).apply {
            hint = "输入工种名称搜索"
            threshold = 0
            setSingleLine(true)
            setAdapter(workTypeAdapter)
            setText(selectedWorkType?.displayText().orEmpty(), false)
            setOnClickListener { showDropDown() }
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDropDown() }
            setOnItemClickListener { _, _, position, _ ->
                selectedWorkType = currentWorkTypes.getOrNull(position)
            }
        }
        val remarkInput = EditText(context).apply {
            hint = "备注"
            setText(team?.remark.orEmpty())
            maxLines = 2
        }
        container.addView(label("班组名称"))
        container.addView(teamNameInput)
        container.addView(label("班组长"))
        container.addView(leaderInput)
        container.addView(label("工种"))
        container.addView(workTypeInput)
        container.addView(label("备注"))
        container.addView(remarkInput)

        leaderInput.bindSearch(
            initialText = selectedLeader?.displayText().orEmpty(),
            onTextChanged = { selectedLeader = null },
            onSearch = { keyword -> repository.loadLeaderOptions(token, projectId, keyword).withCurrentLeader(team) },
            onResult = { options ->
                currentLeaders = options
                leaderAdapter.replaceAll(options.map { it.displayText() })
            },
        )
        workTypeInput.bindSearch(
            initialText = selectedWorkType?.displayText().orEmpty(),
            onTextChanged = { selectedWorkType = null },
            onSearch = { keyword -> repository.loadWorkTypeOptions(token, projectId, keyword).withCurrentWorkType(team) },
            onResult = { options ->
                currentWorkTypes = options
                workTypeAdapter.replaceAll(options.map { it.displayText() })
            },
        )

        AlertDialog.Builder(context)
            .setTitle(if (team == null) "新增班组" else "编辑班组")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = teamNameInput.text?.toString()?.trim().orEmpty()
                        if (name.isBlank()) {
                            toast("请输入班组名称")
                            return@setOnClickListener
                        }
                        val leader = selectedLeader ?: currentLeaders.findByDisplayText(leaderInput.text?.toString())
                        if (leader == null || leader.leaderId <= 0L) {
                            toast("请先从搜索结果选择班组长")
                            return@setOnClickListener
                        }
                        val workTypeText = workTypeInput.text?.toString()?.trim().orEmpty()
                        val workType = selectedWorkType ?: currentWorkTypes.findByDisplayText(workTypeText)
                        if (workTypeText.isNotBlank() && workType == null) {
                            toast("请从搜索结果选择工种")
                            return@setOnClickListener
                        }
                        saveTeam(
                            team = team,
                            teamName = name,
                            leaderId = leader.leaderId,
                            workTypeId = workType?.workTypeId,
                            remark = remarkInput.text?.toString()?.trim().orEmpty(),
                            onComplete = { dialog.dismiss() },
                        )
                    }
                }
            }
            .show()
    }

    private fun <T> AutoCompleteTextView.bindSearch(
        initialText: String,
        onTextChanged: () -> Unit,
        onSearch: suspend (String) -> List<T>,
        onResult: (List<T>) -> Unit,
    ) {
        var searchJob: Job? = null
        addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: android.text.Editable?) {
                val keyword = s?.toString()?.trim().orEmpty()
                if (keyword == initialText) {
                    return
                }
                onTextChanged()
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    runCatching { onSearch(keyword) }
                        .onSuccess { options ->
                            onResult(options)
                            if (hasFocus()) {
                                showDropDown()
                            }
                        }
                }
            }
        })
    }

    private fun ArrayAdapter<String>.replaceAll(values: List<String>) {
        clear()
        addAll(values)
        notifyDataSetChanged()
    }

    private fun label(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setPadding(0, 12, 0, 2)
        }
    }

    private fun saveTeam(
        team: ManagerProjectTeam?,
        teamName: String,
        leaderId: Long,
        workTypeId: Long?,
        remark: String,
        onComplete: () -> Unit,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val token = currentToken()
                if (team == null) {
                    repository.createTeam(token, projectId, teamName, leaderId, workTypeId, remark)
                } else {
                    repository.updateTeam(token, projectId, team.teamId, teamName, leaderId, workTypeId, remark)
                }
            }.onSuccess {
                toast("已保存")
                onComplete()
                loadTeams()
            }.onFailure { throwable ->
                toast(throwable.message ?: "班组保存失败")
            }
        }
    }

    private fun toggleStatus(team: ManagerProjectTeam) {
        val target = if (team.enabled) "DISABLED" else "ENABLED"
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.updateTeamStatus(currentToken(), projectId, team.teamId, target)
            }.onSuccess {
                toast(if (target == "ENABLED") "已启用" else "已停用")
                loadTeams()
            }.onFailure { throwable ->
                toast(throwable.message ?: "状态更新失败")
            }
        }
    }

    private fun List<ManagerLeaderOption>.withCurrentLeader(team: ManagerProjectTeam?): List<ManagerLeaderOption> {
        if (team == null || team.leaderId <= 0L || any { it.leaderId == team.leaderId }) {
            return this
        }
        return listOf(
            ManagerLeaderOption(
                leaderId = team.leaderId,
                leaderName = team.leaderName,
                mobile = "",
            ),
        ) + this
    }

    private fun List<ManagerWorkTypeOption>.withCurrentWorkType(team: ManagerProjectTeam?): List<ManagerWorkTypeOption> {
        if (team?.workTypeId == null || any { it.workTypeId == team.workTypeId }) {
            return this
        }
        return listOf(
            ManagerWorkTypeOption(
                workTypeId = team.workTypeId,
                workTypeName = team.workTypeName,
            ),
        ) + this
    }

    private fun ManagerLeaderOption.displayText(): String {
        val name = leaderName.ifBlank { "班组长 $leaderId" }
        return if (mobile.isBlank()) name else "$name  $mobile"
    }

    private fun ManagerWorkTypeOption.displayText(): String {
        return workTypeName.ifBlank { "工种 $workTypeId" }
    }

    private fun <T> List<T>.findByDisplayText(text: String?): T? {
        val normalized = text?.trim().orEmpty()
        if (normalized.isBlank()) {
            return null
        }
        return firstOrNull { option ->
            when (option) {
                is ManagerLeaderOption -> option.displayText() == normalized
                is ManagerWorkTypeOption -> option.displayText() == normalized
                else -> option.toString() == normalized
            }
        }
    }

    private data class TeamDialogOptions(
        val token: String,
        val leaders: List<ManagerLeaderOption>,
        val workTypes: List<ManagerWorkTypeOption>,
    )

    private suspend fun currentToken(): String {
        return sessionStore.sessionFlow.first().accessToken.takeIf { it.isNotBlank() }
            ?: error("请先登录")
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerTeams.adapter = null
        _binding = null
    }

    companion object {
        private const val ARG_PROJECT_ID = "projectId"

        fun newInstance(projectId: Long): ProjectTeamManageFragment {
            return ProjectTeamManageFragment().apply {
                arguments = Bundle().apply { putLong(ARG_PROJECT_ID, projectId) }
            }
        }
    }
}
