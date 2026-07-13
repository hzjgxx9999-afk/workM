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
import com.qkzc.workerm.data.dispatch.DispatchCreateReq
import com.qkzc.workerm.data.dispatch.DispatchProcessNodeInput
import com.qkzc.workerm.data.dispatch.DispatchRepository
import com.qkzc.workerm.data.project.ManagerProject
import com.qkzc.workerm.data.project.ManagerProjectRepository
import com.qkzc.workerm.data.project.ManagerProjectTeam
import com.qkzc.workerm.data.project.ProjectTeamRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityDispatchCreateBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DispatchCreateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDispatchCreateBinding
    private val repository = DispatchRepository()
    private val projectRepository = ManagerProjectRepository()
    private val teamRepository = ProjectTeamRepository()
    private val sessionStore by lazy { SessionStore(applicationContext) }

    private var projects: List<ManagerProject> = emptyList()
    private var teams: List<ManagerProjectTeam> = emptyList()
    private var selectedProject: ManagerProject? = null
    private var selectedTeam: ManagerProjectTeam? = null
    private var contextLoadJob: Job? = null
    private var coverLoadJob: Job? = null
    private val preselectedProjectId: Long by lazy { intent.getLongExtra(EXTRA_PROJECT_ID, 0L) }
    private val preselectedTeamId: Long by lazy { intent.getLongExtra(EXTRA_TEAM_ID, 0L) }
    private val preselectedLeaderId: Long by lazy { intent.getLongExtra(EXTRA_LEADER_ID, 0L) }

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
        binding.autoFillRequirementButton.setOnClickListener { confirmRequirementTemplateFill() }

        listOf(
            binding.drawingAttachmentButton,
            binding.noticeAttachmentButton,
            binding.photoAttachmentButton,
        ).forEach { view -> view.setOnClickListener { toast("附件上传将在派工单创建后接入") } }
        binding.saveDraftButton.setOnClickListener {
            toast("后端暂未提供派工单草稿接口，请确认派工后创建")
        }
        binding.submitButton.setOnClickListener { submitDispatch() }

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
                val selected = loaded.firstOrNull { it.projectId == preselectedProjectId } ?: loaded.firstOrNull()
                if (selected == null) showEmptyProject() else selectProject(selected)
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
                teamRepository.loadTeams(token, projectId)
                    .filter { it.enabled && it.leaderId > 0L }
            }.onSuccess { loadedTeams ->
                if (selectedProject?.projectId != projectId) return@onSuccess
                teams = loadedTeams
                bindTeamOptions()
            }.onFailure {
                if (selectedProject?.projectId != projectId) return@onFailure
                teams = emptyList()
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
            selectTeam(preselectedTeamIndex().takeIf { it >= 0 } ?: 0)
        }
    }

    private fun preselectedTeamIndex(): Int {
        val selectedProjectId = selectedProject?.projectId ?: return -1
        if (preselectedProjectId > 0L && selectedProjectId != preselectedProjectId) return -1
        preselectedTeamId.takeIf { it > 0L }?.let { teamId ->
            teams.indexOfFirst { it.teamId == teamId }.takeIf { it >= 0 }?.let { return it }
        }
        preselectedLeaderId.takeIf { it > 0L }?.let { leaderId ->
            teams.indexOfFirst { it.leaderId == leaderId }.takeIf { it >= 0 }?.let { return it }
        }
        return -1
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
            return
        }
        binding.teamLeaderText.text = "班组长：${team.leaderName.ifBlank { "--" }}"
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

    private fun confirmRequirementTemplateFill() {
        val hasExistingText = listOf(
            binding.contentInput,
            binding.constructionRequirementInput,
            binding.safetyNoticeInput,
        ).any { inputText(it).isNotBlank() }
        if (!hasExistingText) {
            applyRequirementTemplate(overwrite = false)
            return
        }
        AlertDialog.Builder(this)
            .setTitle("生成任务要求")
            .setMessage("已填写的任务要求是否覆盖？")
            .setPositiveButton("覆盖重写") { _, _ -> applyRequirementTemplate(overwrite = true) }
            .setNegativeButton("仅补空白") { _, _ -> applyRequirementTemplate(overwrite = false) }
            .show()
    }

    private fun applyRequirementTemplate(overwrite: Boolean) {
        val template = DispatchRequirementTemplate.build(
            dispatchType = selectedDispatchType(),
            title = inputText(binding.titleInput),
            location = inputText(binding.locationInput),
        )
        fillTextIfNeeded(binding.contentInput, template.content, overwrite)
        fillTextIfNeeded(binding.constructionRequirementInput, template.constructionRequirement, overwrite)
        fillTextIfNeeded(binding.safetyNoticeInput, template.safetyNotice, overwrite)
        toast("已生成任务要求，可继续修改")
    }

    private fun fillTextIfNeeded(target: TextView, text: String, overwrite: Boolean) {
        if (overwrite || inputText(target).isBlank()) {
            target.text = text
        }
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

    private fun submitDispatch() {
        val request = buildCreateRequest() ?: return
        lifecycleScope.launch {
            binding.saveDraftButton.isEnabled = false
            binding.submitButton.isEnabled = false
            runCatching {
                val token = sessionStore.sessionFlow.first().accessToken
                repository.create(token, request)
            }.onSuccess { order ->
                toast("派工单已创建")
                if (order.id > 0L) {
                    startActivity(DispatchDetailActivity.intent(this@DispatchCreateActivity, order.id))
                }
                finish()
            }.onFailure { throwable ->
                toast(throwable.message ?: "派工失败")
                binding.saveDraftButton.isEnabled = true
                binding.submitButton.isEnabled = true
            }
        }
    }

    private fun buildCreateRequest(): DispatchCreateReq? {
        val project = selectedProject ?: run {
            toast("请选择项目")
            return null
        }
        val team = selectedTeam ?: run {
            toast("请选择班组")
            return null
        }
        val title = inputText(binding.titleInput)
        val location = inputText(binding.locationInput)
        val deadline = inputText(binding.deadlineInput)
        val content = inputText(binding.contentInput)
        if (title.isBlank()) {
            toast("请输入派工标题")
            binding.titleInput.requestFocus()
            return null
        }
        if (location.isBlank()) {
            toast("请输入施工位置")
            binding.locationInput.requestFocus()
            return null
        }
        if (deadline.isBlank()) {
            toast("请选择截止时间")
            return null
        }
        if (content.isBlank()) {
            toast("请输入任务内容")
            binding.contentInput.requestFocus()
            return null
        }

        val processEnabled = binding.processCheckSwitch.isChecked
        val processNodes = if (processEnabled) buildProcessNodes() ?: return null else emptyList()
        val completionPhotoRequired = binding.completionPhotoRequiredSwitch.isChecked
        return DispatchCreateReq(
            projectId = project.projectId,
            teamId = team.teamId,
            leaderId = team.leaderId,
            title = title,
            dispatchType = selectedDispatchType(),
            priority = selectedPriority(),
            content = content,
            locationDesc = location,
            deadlineTime = deadline,
            constructionRequirement = optionalInputText(binding.constructionRequirementInput),
            safetyNotice = optionalInputText(binding.safetyNoticeInput),
            acceptanceStandard = optionalInputText(binding.acceptanceStandardInput),
            processCheckEnabled = processEnabled,
            processCheckMode = if (processEnabled) selectedProcessMode() else null,
            processCheckDescription = if (processEnabled) optionalInputText(binding.processDescriptionInput) else null,
            processNodes = processNodes,
            processMinPhotoCount = if (processEnabled) countValue(binding.processMinPhotoInput) else 0,
            processRequireLocation = processEnabled && binding.processRequireLocationSwitch.isChecked,
            managerSpotCheckEnabled = processEnabled && binding.managerSpotCheckSwitch.isChecked,
            beforePhotoRequired = binding.beforePhotoRequiredSwitch.isChecked,
            completionPhotoRequired = completionPhotoRequired,
            completionMinPhotoCount = if (completionPhotoRequired) countValue(binding.completionMinPhotoInput) else 0,
        )
    }

    private fun buildProcessNodes(): List<DispatchProcessNodeInput>? {
        val requiredPhotoCount = countValue(binding.processMinPhotoInput).coerceAtLeast(MIN_PHOTO_COUNT)
        val requiredLocation = if (binding.processRequireLocationSwitch.isChecked) 1 else 0
        val description = optionalInputText(binding.processDescriptionInput)
        val nodes = listOf(
            binding.processNodeOpening to "开工确认",
            binding.processNodeMiddle to "施工中检查",
            binding.processNodeHidden to "隐蔽前检查",
            binding.processNodeFinal to "完工前复核",
        ).mapIndexedNotNull { index, (checkBox, nodeName) ->
            if (!checkBox.isChecked) {
                null
            } else {
                DispatchProcessNodeInput(
                    nodeName = nodeName,
                    nodeSort = index + 1,
                    checkType = "NODE",
                    requiredPhotoCount = requiredPhotoCount,
                    requiredLocation = requiredLocation,
                    reviewerRole = "TEAM_LEADER",
                    description = description,
                )
            }
        }
        if (nodes.isEmpty()) {
            toast("请至少选择一个过程检查节点")
            return null
        }
        return nodes
    }

    private fun selectedDispatchType(): String = when (binding.dispatchTypeGroup.checkedButtonId) {
        R.id.type_repair_button -> "REPAIR"
        R.id.type_install_button -> "INSTALL"
        R.id.type_clean_button -> "CLEAN"
        R.id.type_rectification_button -> "RECTIFICATION"
        R.id.type_inspection_button -> "INSPECTION"
        else -> "CONSTRUCTION"
    }

    private fun selectedPriority(): String = when (binding.priorityGroup.checkedButtonId) {
        R.id.priority_urgent_button -> "URGENT"
        R.id.priority_major_button -> "MAJOR"
        else -> "NORMAL"
    }

    private fun selectedProcessMode(): String = when (binding.processModeSpinner.checkedButtonId) {
        R.id.process_mode_day_button -> "DAY"
        R.id.process_mode_manual_button -> "MANUAL"
        else -> "NODE"
    }

    private fun inputText(view: TextView): String = view.text?.toString().orEmpty().trim()

    private fun optionalInputText(view: TextView): String? = inputText(view).ifBlank { null }

    private fun countValue(view: TextView): Int =
        view.text?.toString().orEmpty().trim().toIntOrNull()?.coerceIn(MIN_PHOTO_COUNT, MAX_PHOTO_COUNT)
            ?: MIN_PHOTO_COUNT

    private fun projectStageText(status: String): String = when (status.uppercase(Locale.ROOT)) {
        "CONSTRUCTION" -> "施工中"
        "PAUSED" -> "已暂停"
        "COMPLETED" -> "已完成"
        else -> status.ifBlank { "未设置" }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_PROJECT_ID = "projectId"
        const val EXTRA_TEAM_ID = "teamId"
        const val EXTRA_LEADER_ID = "leaderId"
        private const val DEADLINE_PATTERN = "yyyy-MM-dd HH:mm:ss"
        private const val MIN_PHOTO_COUNT = 1
        private const val MAX_PHOTO_COUNT = 99
    }
}
