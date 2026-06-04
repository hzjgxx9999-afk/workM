package com.qkzc.workerm.ui.project

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.data.project.ManagerProjectRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityProjectDetailBinding
import com.qkzc.workerm.ui.video.DrawingDocsActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProjectDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjectDetailBinding
    private val projectRepository = ManagerProjectRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProjectDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
        binding.drawingDocsAction.setOnClickListener {
            startActivity(Intent(this, DrawingDocsActivity::class.java))
        }
        loadProject()
    }

    private fun loadProject() {
        lifecycleScope.launch {
            runCatching {
                val session = SessionStore(this@ProjectDetailActivity).sessionFlow.first()
                val projectId = intent.getLongExtra(EXTRA_PROJECT_ID, 0L)
                    .takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                projectRepository.loadProjectDetail(session.accessToken, projectId)
            }.onSuccess { project ->
                binding.projectNameText.text = project.projectName.ifBlank { "项目详情" }
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
            }.onFailure { throwable ->
                Toast.makeText(
                    this@ProjectDetailActivity,
                    throwable.message ?: "项目详情加载失败",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    companion object {
        const val EXTRA_PROJECT_ID = "projectId"
    }
}
