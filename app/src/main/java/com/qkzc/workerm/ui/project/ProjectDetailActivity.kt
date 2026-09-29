package com.qkzc.workerm.ui.project

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.BuildConfig
import com.qkzc.workerm.R
import com.qkzc.workerm.data.dispatch.DispatchRepository
import com.qkzc.workerm.data.project.ManagerProject
import com.qkzc.workerm.data.project.ManagerProjectFile
import com.qkzc.workerm.data.project.ManagerProjectRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityProjectDetailBinding
import com.qkzc.workerm.ui.dispatch.DispatchCreateActivity
import com.qkzc.workerm.ui.dispatch.DispatchDetailActivity
import com.qkzc.workerm.ui.dispatch.DispatchListActivity
import com.qkzc.workerm.ui.dispatch.ManagerDispatchAdapter
import com.qkzc.workerm.ui.invite.InviteCodeManageActivity
import com.qkzc.workerm.ui.location.ProjectGeofenceActivity
import com.qkzc.workerm.ui.video.DrawingDocsActivity
import com.qkzc.workerm.ui.worker.ProjectMemberManageActivity
import com.qkzc.workerm.ui.worker.WorkerScanActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProjectDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjectDetailBinding
    private val projectRepository = ManagerProjectRepository()
    private val dispatchRepository = DispatchRepository()
    private val dispatchAdapter = ManagerDispatchAdapter { order ->
        startActivity(DispatchDetailActivity.intent(this, order.id))
    }
    private var currentProjectId: Long = 0L
    private var currentAccessToken: String = ""
    private var currentProjectName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProjectDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.projectDispatchRecycler.layoutManager = LinearLayoutManager(this)
        binding.projectDispatchRecycler.adapter = dispatchAdapter
        ViewCompat.setOnApplyWindowInsetsListener(binding.detailRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            insets
        }
        binding.detailBackButton.setOnClickListener { finish() }
        binding.projectMaterialsAction.setOnClickListener {
            openProjectScoped(ProjectMaterialsActivity::class.java)
        }
        binding.drawingDocsAction.setOnClickListener {
            openProjectScoped(DrawingDocsActivity::class.java)
        }
        binding.constructionLogAction.setOnClickListener {
            openProjectScoped(ConstructionLogActivity::class.java)
        }
        binding.projectReportAction.setOnClickListener {
            openProjectScoped(ProjectReportActivity::class.java)
        }
        binding.projectGeofenceAction.setOnClickListener {
            openProjectScoped(ProjectGeofenceActivity::class.java)
        }
        binding.projectMembersAction.setOnClickListener {
            openProjectScoped(ProjectMemberManageActivity::class.java)
        }
        binding.inviteCodeAction.setOnClickListener {
            openProjectScoped(InviteCodeManageActivity::class.java)
        }
        binding.qrScanAction.setOnClickListener {
            openProjectScoped(WorkerScanActivity::class.java)
        }
        binding.projectDispatchAllAction.setOnClickListener {
            openProjectDispatchList()
        }
        binding.projectDispatchCreateButton.setOnClickListener {
            openProjectDispatchCreate()
        }
        loadProject()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized && currentProjectId > 0L && currentAccessToken.isNotBlank()) {
            loadProjectDispatches()
        }
    }

    private fun loadProject() {
        lifecycleScope.launch {
            runCatching {
                val session = SessionStore(this@ProjectDetailActivity).sessionFlow.first()
                currentAccessToken = session.accessToken
                val projectId = intent.getLongExtra(EXTRA_PROJECT_ID, 0L)
                    .takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                currentProjectId = projectId
                projectRepository.loadProjectDetail(session.accessToken, projectId)
            }.onSuccess { project ->
                renderProject(project)
                loadWorkerStats()
                loadProjectDispatches()
            }.onFailure { throwable ->
                Toast.makeText(
                    this@ProjectDetailActivity,
                    throwable.message ?: "项目详情加载失败",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun renderProject(project: ManagerProject) {
        binding.projectNameText.text = project.projectName.ifBlank { "项目详情" }
        currentProjectName = project.projectName
        binding.projectAddressText.text = project.projectAddress.ifBlank { "暂无项目地址" }
        binding.projectStatusText.text = project.projectStatus.toStatusText()
        binding.projectProgressText.text = "${project.progressPercent}%"
        binding.projectProgressBar.progress = project.progressPercent
        binding.projectDaysText.text = project.daysToFinish?.let { "距竣工 ${it}天" } ?: "未设竣工"
        binding.projectPlanText.text = listOfNotNull(
            project.plannedFinishDate.takeIf { it.isNotBlank() }?.let { "计划竣工 $it" },
            project.projectCode.takeIf { it.isNotBlank() }?.let { "编号 $it" },
            project.projectManagerName.takeIf { it.isNotBlank() }?.let { "经理 $it" },
            "在场 ${project.workerCount}人",
            "待处理风险 ${project.unhandledRiskCount}条",
        ).joinToString("  ")
        renderRecentFiles(project.recentFiles)
        loadCover(project)
    }

    private fun loadWorkerStats() {
        val token = currentAccessToken.takeIf { it.isNotBlank() } ?: return
        val projectId = currentProjectId.takeIf { it > 0L } ?: return
        lifecycleScope.launch {
            runCatching {
                projectRepository.loadWorkerStats(token, projectId)
            }.onSuccess { stats ->
                val base = binding.projectPlanText.text.toString().substringBefore("\n在场统计")
                binding.projectPlanText.text = "$base\n在场统计 ${stats.totalCurrentCount}人：在场${stats.activeCount}，入场中${stats.enteringCount}，已绑定${stats.boundCount}"
            }
        }
    }

    private fun renderRecentFiles(files: List<ManagerProjectFile>) {
        binding.recentFilesContainer.removeAllViews()
        binding.recentFilesEmptyText.isVisible = files.isEmpty()
        files.forEach { file ->
            binding.recentFilesContainer.addView(file.toRow())
        }
    }

    private fun ManagerProjectFile.toRow(): TextView {
        return TextView(this@ProjectDetailActivity).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 8.dp()
            }
            minHeight = 50.dp()
            setPadding(12.dp(), 8.dp(), 12.dp(), 8.dp())
            text = buildString {
                append(fileName.ifBlank { "未命名文件" })
                fileType.takeIf { it.isNotBlank() }?.let { append("  $it") }
                createTime.takeIf { it.isNotBlank() }?.let { append("\n$it") }
            }
            setTextColor(getColor(R.color.text_primary))
            textSize = 13f
            background = getDrawable(R.drawable.bg_search_pill)
        }
    }

    private fun loadCover(project: ManagerProject) {
        val token = currentAccessToken.takeIf { it.isNotBlank() }
        val projectId = project.projectId.takeIf { it > 0L } ?: currentProjectId.takeIf { it > 0L }
        if (project.coverImageUrl.isBlank() || token == null || projectId == null) {
            binding.projectCoverImage.setImageResource(R.drawable.detail_header_crane)
            return
        }
        logCover("preview projectId=$projectId")
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = projectRepository.loadProjectCoverBytes(token, projectId)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: error("项目封面解码失败: bytes=${bytes.size}")
                }
            }.onSuccess { bitmap ->
                binding.projectCoverImage.setImageBitmap(bitmap)
            }.onFailure {
                logCover("failed projectId=$projectId", it)
                binding.projectCoverImage.setImageResource(R.drawable.detail_header_crane)
            }
        }
    }

    private fun logCover(message: String, throwable: Throwable? = null) {
        if (!BuildConfig.DEBUG) return
        if (throwable == null) {
            Log.d(TAG, "cover $message")
        } else {
            Log.e(TAG, "cover $message", throwable)
        }
    }

    private fun openProjectScoped(activityClass: Class<*>) {
        val projectId = currentProjectId.takeIf { it > 0L }
        if (projectId == null) {
            Toast.makeText(this, "缺少项目 ID", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, activityClass).putExtra(EXTRA_PROJECT_ID, projectId))
    }

    private fun openProjectDispatchList() {
        val projectId = currentProjectId.takeIf { it > 0L }
        if (projectId == null) {
            Toast.makeText(this, "缺少项目 ID", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            DispatchListActivity.intent(
                context = this,
                projectId = projectId,
                projectName = currentProjectName,
            ),
        )
    }

    private fun openProjectDispatchCreate() {
        val projectId = currentProjectId.takeIf { it > 0L }
        if (projectId == null) {
            Toast.makeText(this, "缺少项目 ID", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, DispatchCreateActivity::class.java)
                .putExtra(DispatchCreateActivity.EXTRA_PROJECT_ID, projectId),
        )
    }

    private fun loadProjectDispatches() {
        val token = currentAccessToken.takeIf { it.isNotBlank() } ?: return
        val projectId = currentProjectId.takeIf { it > 0L } ?: return
        lifecycleScope.launch {
            runCatching {
                dispatchRepository.recent(token = token, projectId = projectId, pageSize = 5)
            }.onSuccess { orders ->
                dispatchAdapter.submitList(orders)
                binding.projectDispatchRecycler.isVisible = orders.isNotEmpty()
                binding.projectDispatchEmptyText.isVisible = orders.isEmpty()
                binding.projectDispatchEmptyText.text = "暂无派工任务"
                binding.projectDispatchEmptyText.setOnClickListener(null)
            }.onFailure {
                dispatchAdapter.submitList(emptyList())
                binding.projectDispatchRecycler.isVisible = false
                binding.projectDispatchEmptyText.isVisible = true
                binding.projectDispatchEmptyText.text = "派工动态加载失败，点击重试"
                binding.projectDispatchEmptyText.setOnClickListener { loadProjectDispatches() }
            }
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "ProjectDetailActivity"
        const val EXTRA_PROJECT_ID = "projectId"
    }
}
