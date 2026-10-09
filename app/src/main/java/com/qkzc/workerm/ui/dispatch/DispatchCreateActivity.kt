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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.qkzc.workerm.ui.common.EdgeToEdgeActivity
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.R
import com.qkzc.workerm.data.dispatch.DispatchCreateReq
import com.qkzc.workerm.data.dispatch.DispatchProcessNodeInput
import com.qkzc.workerm.data.dispatch.DispatchRepository
import com.qkzc.workerm.data.dispatch.DispatchWorkOrderLocationInput
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

class DispatchCreateActivity : EdgeToEdgeActivity() {
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
    private var selectedLocationCoordinates: Pair<Double, Double>? = null
    private var currentStep = STEP_TASK
    private val preselectedProjectId: Long by lazy { intent.getLongExtra(EXTRA_PROJECT_ID, 0L) }
    private val preselectedTeamId: Long by lazy { intent.getLongExtra(EXTRA_TEAM_ID, 0L) }
    private val preselectedLeaderId: Long by lazy { intent.getLongExtra(EXTRA_LEADER_ID, 0L) }
    private val locationPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val latitude = result.data?.getDoubleExtra(DispatchLocationPickerActivity.EXTRA_LATITUDE, Double.NaN) ?: Double.NaN
        val longitude = result.data?.getDoubleExtra(DispatchLocationPickerActivity.EXTRA_LONGITUDE, Double.NaN) ?: Double.NaN
        if (!latitude.isFinite() || !longitude.isFinite()) return@registerForActivityResult
        selectedLocationCoordinates = longitude to latitude
        if (inputText(binding.locationInput).isBlank()) {
            binding.locationInput.setText("施工点 %.6f, %.6f".format(Locale.CHINA, latitude, longitude))
        }
        toast("已选取施工坐标，可继续补充位置名称")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDispatchCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdge(
            root = binding.root,
            topBarId = R.id.dispatch_create_top_bar,
            bottomBarId = R.id.dispatch_create_action_bar,
        )
        configureSystemBarIconAppearance(
            lightStatusBars = true,
            lightNavigationBars = true,
        )

        binding.backButton.setOnClickListener {
            if (currentStep == STEP_TASK) finish() else moveToStep(currentStep - 1)
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentStep == STEP_TASK) finish() else moveToStep(currentStep - 1)
            }
        })
        binding.previousButton.setOnClickListener { moveToStep(currentStep - 1) }
        binding.switchProjectButton.setOnClickListener { showProjectSelector() }
        binding.deadlineInput.setOnClickListener { showDeadlinePicker() }
        binding.deadlineInput.setOnLongClickListener { true }
        binding.pickLocationButton.setOnClickListener {
            locationPickerLauncher.launch(DispatchLocationPickerActivity.intent(this))
        }
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

        binding.attachmentCard.visibility = View.GONE
        binding.saveDraftButton.visibility = View.GONE
        binding.submitButton.setOnClickListener {
            when (currentStep) {
                STEP_TASK -> if (validateTaskStep()) moveToStep(STEP_REQUIREMENTS)
                STEP_REQUIREMENTS -> if (validateRequirementsStep()) {
                    updateReviewSummary()
                    moveToStep(STEP_REVIEW)
                }
                STEP_REVIEW -> submitDispatch()
            }
        }

        applyFixedRequirements()
        moveToStep(STEP_TASK)
        loadProjects()
    }

    private fun applyFixedRequirements() {
        binding.processCheckSwitch.isChecked = true
        binding.processCheckSwitch.isEnabled = false
        binding.completionPhotoRequiredSwitch.isChecked = true
        binding.completionPhotoRequiredSwitch.isEnabled = false
        binding.beforePhotoRequiredSwitch.isChecked = true
        binding.beforePhotoRequiredSwitch.isEnabled = false
        binding.processRequireLocationSwitch.isChecked = true
        binding.managerSpotCheckSwitch.isChecked = true
        listOf(
            binding.processNodeOpening,
            binding.processNodeMiddle,
            binding.processNodeHidden,
            binding.processNodeFinal,
        ).forEach {
            it.isChecked = true
            it.isEnabled = false
        }
        setProcessSectionVisible(true)
        setCompletionCountEnabled(true)
    }

    private fun moveToStep(step: Int) {
        currentStep = step.coerceIn(STEP_TASK, STEP_REVIEW)
        val isTaskStep = currentStep == STEP_TASK
        val isRequirementsStep = currentStep == STEP_REQUIREMENTS
        binding.projectCard.visibility = if (isTaskStep) View.VISIBLE else View.GONE
        binding.taskCard.visibility = if (isTaskStep) View.VISIBLE else View.GONE
        binding.teamCard.visibility = if (isTaskStep) View.VISIBLE else View.GONE
        binding.requirementsCard.visibility = if (isRequirementsStep) View.VISIBLE else View.GONE
        binding.acceptanceCard.visibility = if (isRequirementsStep) View.VISIBLE else View.GONE
        binding.processCard.visibility = if (isRequirementsStep) View.VISIBLE else View.GONE
        binding.reviewCard.visibility = if (currentStep == STEP_REVIEW) View.VISIBLE else View.GONE
        binding.previousButton.visibility = if (isTaskStep) View.GONE else View.VISIBLE
        binding.submitButton.text = if (currentStep == STEP_REVIEW) "确认派工" else "下一步"
        binding.stepTitleText.text = when (currentStep) {
            STEP_TASK -> "第 1 步 · 填写任务"
            STEP_REQUIREMENTS -> "第 2 步 · 确认施工要求"
            else -> "第 3 步 · 检查并派工"
        }
        binding.stepSubtitleText.text = when (currentStep) {
            STEP_TASK -> "写清楚做什么、在哪里做，再选好施工班组。"
            STEP_REQUIREMENTS -> "照片和过程检查由系统安排，补充现场要求即可。"
            else -> "核对任务内容和施工安排，确认后发送给施工班组。"
        }
        binding.root.findViewById<androidx.core.widget.NestedScrollView>(R.id.dispatch_create_scroll_view)
            ?.smoothScrollTo(0, 0)
    }

    private fun validateTaskStep(): Boolean {
        if (selectedProject == null) {
            toast("请先选择项目")
            return false
        }
        if (inputText(binding.titleInput).isBlank()) {
            toast("请填写任务名称")
            binding.titleInput.requestFocus()
            return false
        }
        if (inputText(binding.locationInput).isBlank()) {
            toast("请填写施工地点")
            binding.locationInput.requestFocus()
            return false
        }
        if (inputText(binding.deadlineInput).isBlank()) {
            toast("请选择完成时间")
            return false
        }
        if (selectedTeam == null) {
            toast("请选择施工班组")
            return false
        }
        return true
    }

    private fun validateRequirementsStep(): Boolean {
        if (inputText(binding.contentInput).isBlank()) {
            toast("请填写需要处理的问题")
            binding.contentInput.requestFocus()
            return false
        }
        return true
    }

    private fun updateReviewSummary() {
        val projectName = selectedProject?.projectName.orEmpty().ifBlank { "--" }
        val teamName = selectedTeam?.teamName.orEmpty().ifBlank { "--" }
        val processDescription = optionalInputText(binding.processDescriptionInput)
        val acceptanceDescription = optionalInputText(binding.acceptanceStandardInput)
        val lines = mutableListOf(
            "项目：$projectName",
            "施工班组：$teamName（班组长：${selectedTeam?.leaderName.orEmpty().ifBlank { "--" }}）",
            "任务：${inputText(binding.titleInput)}",
            "类型：${dispatchTypeLabel()} · 优先级：${priorityLabel()}",
            "地点：${inputText(binding.locationInput)}",
            "完成时间：${inputText(binding.deadlineInput)}",
            "\n需要处理：${inputText(binding.contentInput)}",
            "施工要求：${optionalInputText(binding.constructionRequirementInput) ?: "无补充要求"}",
            "安全提醒：${optionalInputText(binding.safetyNoticeInput) ?: "无补充提醒"}",
            "\n开工前：必须拍摄现场照片",
            "完工后：至少上传 ${countValue(binding.completionMinPhotoInput)} 张照片",
            "施工中：按开工、施工中、隐蔽前、完工前 4 个节点检查；每个节点拍 2 张照片并记录位置",
        )
        if (processDescription != null) lines += "过程检查补充：$processDescription"
        if (acceptanceDescription != null) lines += "验收重点：$acceptanceDescription"
        binding.reviewSummaryText.text = lines.joinToString("\n")
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

        val processNodes = buildProcessNodes()
        val completionPhotoRequired = true
        val locationCoordinates = selectedLocationCoordinates
        return DispatchCreateReq(
            projectId = project.projectId,
            teamId = team.teamId,
            leaderId = team.leaderId,
            title = title,
            dispatchType = selectedDispatchType(),
            priority = selectedPriority(),
            content = content,
            locationDesc = location,
            location = locationCoordinates?.let { (longitude, latitude) ->
                DispatchWorkOrderLocationInput(
                    locationName = location,
                    longitude = longitude,
                    latitude = latitude,
                    locationDescription = location,
                )
            },
            deadlineTime = deadline,
            constructionRequirement = optionalInputText(binding.constructionRequirementInput),
            safetyNotice = optionalInputText(binding.safetyNoticeInput),
            acceptanceStandard = optionalInputText(binding.acceptanceStandardInput),
            processCheckEnabled = true,
            processCheckMode = "NODE",
            processCheckDescription = optionalInputText(binding.processDescriptionInput),
            processNodes = processNodes,
            processMinPhotoCount = DEFAULT_PROCESS_PHOTO_COUNT,
            processRequireLocation = true,
            managerSpotCheckEnabled = true,
            beforePhotoRequired = true,
            completionPhotoRequired = completionPhotoRequired,
            completionMinPhotoCount = countValue(binding.completionMinPhotoInput),
        )
    }

    private fun buildProcessNodes(): List<DispatchProcessNodeInput> {
        val requiredPhotoCount = DEFAULT_PROCESS_PHOTO_COUNT
        val requiredLocation = 1
        val description = optionalInputText(binding.processDescriptionInput)
        return listOf("开工确认", "施工中检查", "隐蔽前检查", "完工前复核")
            .mapIndexed { index, nodeName ->
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

    private fun dispatchTypeLabel(): String = when (selectedDispatchType()) {
        "REPAIR" -> "维修"
        "INSTALL" -> "安装"
        "CLEAN" -> "清理"
        "RECTIFICATION" -> "整改"
        "INSPECTION" -> "巡检"
        else -> "施工"
    }

    private fun priorityLabel(): String = when (selectedPriority()) {
        "URGENT" -> "紧急"
        "MAJOR" -> "重大"
        else -> "普通"
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
        private const val STEP_TASK = 1
        private const val STEP_REQUIREMENTS = 2
        private const val STEP_REVIEW = 3
        private const val DEFAULT_PROCESS_PHOTO_COUNT = 2
    }
}
