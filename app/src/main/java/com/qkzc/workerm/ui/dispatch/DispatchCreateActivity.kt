package com.qkzc.workerm.ui.dispatch

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.R
import com.qkzc.workerm.data.project.ManagerProject
import com.qkzc.workerm.data.project.ManagerProjectRepository
import com.qkzc.workerm.data.project.ManagerProjectTeam
import com.qkzc.workerm.data.project.ProjectTeamRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.data.worker.ManagerWorker
import com.qkzc.workerm.data.worker.ManagerWorkerRepository
import com.qkzc.workerm.databinding.ActivityDispatchCreateBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DispatchCreateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDispatchCreateBinding
    private val projectRepository = ManagerProjectRepository()
    private val teamRepository = ProjectTeamRepository()
    private val workerRepository = ManagerWorkerRepository()
    private val sessionStore by lazy { SessionStore(applicationContext) }

    private var projects: List<ManagerProject> = emptyList()
    private var teams: List<ManagerProjectTeam> = emptyList()
    private var projectWorkers: List<ManagerWorker> = emptyList()
    private var selectedProject: ManagerProject? = null
    private var selectedTeam: ManagerProjectTeam? = null
    private var contextLoadJob: Job? = null
    private var coverLoadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        binding = ActivityDispatchCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.switchProjectButton.setOnClickListener { showProjectSelector() }
        binding.deadlineInput.setOnClickListener { showDeadlinePicker() }
        binding.deadlineInput.setOnLongClickListener { true }
        binding.processCheckSwitch.setOnCheckedChangeListener { _, checked ->
            setProcessSectionVisible(checked)
        }
        binding.completionPhotoRequiredSwitch.setOnCheckedChangeListener { _, checked ->
            setCompletionCountEnabled(checked)
        }
        binding.completionMinusButton.setOnClickListener {
            changeCount(binding.completionMinPhotoInput, -1)
        }
        binding.completionPlusButton.setOnClickListener {
            changeCount(binding.completionMinPhotoInput, 1)
        }
        binding.processMinusButton.setOnClickListener {
            changeCount(binding.processMinPhotoInput, -1)
        }
        binding.processPlusButton.setOnClickListener {
            changeCount(binding.processMinPhotoInput, 1)
        }
        binding.teamSpinner.setOnItemClickListener { _, _, position, _ -> selectTeam(position) }

        listOf(
            binding.drawingAttachmentButton,
            binding.noticeAttachmentButton,
            binding.photoAttachmentButton,
            binding.saveDraftButton,
            binding.submitButton,
        ).forEach { view -> view.setOnClickListener { showUiOnlyMessage() } }

        setProcessSectionVisible(binding.processCheckSwitch.isChecked)
        setCompletionCountEnabled(binding.completionPhotoRequiredSwitch.isChecked)
        loadProjects()
    }

    private fun loadProjects() {
        lifecycleScope.launch {
            runCatching {
                val token = sessionStore.sessionFlow.first().accessToken
                projectRepository.loadProjects(token)
            }.onSuccess { loaded ->
                projects = loaded
                if (loaded.isEmpty()) showEmptyProject() else selectProject(loaded.first())
            }.onFailure {
                showEmptyProject()
                toast(it.message ?: "项目加载失败")
            }
        }
    }

    private fun showProjectSelector() {
        if (projects.isEmpty()) {
            toast("暂无可切换项目")
            return
        }
        val checked = projects.indexOfFirst { it.projectId == selectedProject?.projectId }
        AlertDialog.Builder(this)
            .setTitle("切换项目")
            .setSingleChoiceItems(projects.map { it.projectName }.toTypedArray(), checked) { dialog, which ->
                projects.getOrNull(which)?.let(::selectProject)
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun selectProject(project: ManagerProject) {
        selectedProject = project
        binding.projectNameText.text = project.projectName.ifBlank { "未命名项目" }
        binding.projectManagerText.text =
            "项目经理：${project.projectManagerName.ifBlank { "--" }}"
        binding.projectStageText.text = "项目阶段：${projectStageText(project.projectStatus)}"
        showProjectCoverPlaceholder()
        loadProjectCover(project.projectId)
        loadProjectTeamsAndWorkers(project.projectId)
    }

    private fun loadProjectTeamsAndWorkers(projectId: Long) {
        contextLoadJob?.cancel()
        contextLoadJob = lifecycleScope.launch {
            runCatching {
                val token = sessionStore.sessionFlow.first().accessToken
                coroutineScope {
                    val teamResult = async {
                        teamRepository.loadTeams(token, projectId)
                            .filter { it.enabled && it.leaderId > 0L }
                    }
                    val workerResult = async { workerRepository.listWorkers(token, projectId) }
                    teamResult.await() to workerResult.await()
                }
            }.onSuccess { (loadedTeams, loadedWorkers) ->
                if (selectedProject?.projectId != projectId) return@onSuccess
                teams = loadedTeams
                projectWorkers = loadedWorkers
                bindTeamOptions()
            }.onFailure {
                if (selectedProject?.projectId != projectId) return@onFailure
                teams = emptyList()
                projectWorkers = emptyList()
                bindTeamOptions()
                toast(it.message ?: "班组信息加载失败")
            }
        }
    }

    private fun bindTeamOptions() {
        binding.teamSpinner.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                teams.map { it.teamName },
            ),
        )
        if (teams.isEmpty()) {
            selectedTeam = null
            binding.teamSpinner.setText("暂无可用班组", false)
            binding.teamSpinner.isEnabled = false
            updateTeamSummary()
        } else {
            binding.teamSpinner.isEnabled = true
            selectTeam(0)
        }
    }

    private fun selectTeam(position: Int) {
        selectedTeam = teams.getOrNull(position)
        binding.teamSpinner.setText(selectedTeam?.teamName.orEmpty(), false)
        updateTeamSummary()
    }

    private fun updateTeamSummary() {
        val team = selectedTeam
        if (team == null) {
            binding.teamLeaderText.text = "班组长：--"
            binding.teamWorkerCountText.text = "预计：--"
            return
        }
        val count = projectWorkers.count { worker ->
            worker.teamId == team.teamId &&
                worker.bindStatus.uppercase(Locale.ROOT) in ACTIVE_BIND_STATUSES
        }
        binding.teamLeaderText.text = "班组长：${team.leaderName.ifBlank { "--" }}"
        binding.teamWorkerCountText.text = "预计：${count}人"
    }

    private fun loadProjectCover(projectId: Long) {
        coverLoadJob?.cancel()
        coverLoadJob = lifecycleScope.launch {
            runCatching {
                val token = sessionStore.sessionFlow.first().accessToken
                projectRepository.loadProjectCoverBytes(token, projectId)
            }.onSuccess { bytes ->
                if (selectedProject?.projectId != projectId) return@onSuccess
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap == null) {
                    showProjectCoverPlaceholder()
                } else {
                    binding.projectCover.setPadding(0, 0, 0, 0)
                    binding.projectCover.scaleType = ImageView.ScaleType.CENTER_CROP
                    binding.projectCover.setImageBitmap(bitmap)
                }
            }.onFailure {
                if (selectedProject?.projectId == projectId) showProjectCoverPlaceholder()
            }
        }
    }

    private fun showProjectCoverPlaceholder() {
        binding.projectCover.setPadding(dp(18), dp(18), dp(18), dp(18))
        binding.projectCover.scaleType = ImageView.ScaleType.CENTER
        binding.projectCover.setImageResource(R.drawable.ic_project)
    }

    private fun showEmptyProject() {
        selectedProject = null
        teams = emptyList()
        projectWorkers = emptyList()
        binding.projectNameText.text = "暂无可管理项目"
        binding.projectManagerText.text = "项目经理：--"
        binding.projectStageText.text = "项目阶段：--"
        showProjectCoverPlaceholder()
        bindTeamOptions()
    }

    private fun setProcessSectionVisible(visible: Boolean) {
        binding.processDetailsContainer.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setCompletionCountEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1f else 0.4f
        listOf(
            binding.completionMinusButton,
            binding.completionMinPhotoInput,
            binding.completionPlusButton,
        ).forEach {
            it.isEnabled = enabled
            it.alpha = alpha
        }
    }

    private fun changeCount(target: TextView, delta: Int) {
        val current = target.text.toString().toIntOrNull() ?: MIN_PHOTO_COUNT
        target.text = (current + delta).coerceIn(MIN_PHOTO_COUNT, MAX_PHOTO_COUNT).toString()
    }

    private fun showDeadlinePicker() {
        val calendar = Calendar.getInstance()
        val current = binding.deadlineInput.text?.toString()?.trim().orEmpty()
        if (current.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"))) {
            runCatching {
                val parsed = SimpleDateFormat(DEADLINE_PATTERN, Locale.CHINA).parse(current) ?: Date()
                calendar.time = parsed
            }
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                showDeadlineTimePicker(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun showDeadlineTimePicker(calendar: Calendar) {
        TimePickerDialog(
            this,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                binding.deadlineInput.setText(
                    SimpleDateFormat(DEADLINE_PATTERN, Locale.CHINA).format(calendar.time),
                )
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true,
        ).show()
    }

    private fun projectStageText(status: String): String = when (status.uppercase(Locale.ROOT)) {
        "CONSTRUCTION" -> "施工中"
        "PAUSED" -> "已暂停"
        "COMPLETED" -> "已完成"
        else -> status.ifBlank { "未设置" }
    }

    private fun showUiOnlyMessage() {
        toast("页面 UI 已完成，业务功能将在下一阶段接入")
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        private const val DEADLINE_PATTERN = "yyyy-MM-dd HH:mm:ss"
        private const val MIN_PHOTO_COUNT = 1
        private const val MAX_PHOTO_COUNT = 99
        private val ACTIVE_BIND_STATUSES = setOf("BOUND", "ENTERING", "ACTIVE")
    }
}
