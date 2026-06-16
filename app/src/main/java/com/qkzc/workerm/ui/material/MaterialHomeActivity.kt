package com.qkzc.workerm.ui.material

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.R
import com.qkzc.workerm.data.material.MaterialCategory
import com.qkzc.workerm.data.material.MaterialOverview
import com.qkzc.workerm.data.material.MaterialRepository
import com.qkzc.workerm.data.project.ManagerProject
import com.qkzc.workerm.data.project.ManagerProjectRepository
import com.qkzc.workerm.data.session.LoginSession
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityMaterialHomeBinding
import com.qkzc.workerm.ui.project.ProjectDetailActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MaterialHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaterialHomeBinding
    private lateinit var sessionStore: SessionStore
    private val materialRepository = MaterialRepository()
    private val projectRepository = ManagerProjectRepository()

    private var currentProjectId: Long = 0L
    private var currentToken: String = ""
    private var currentSession: LoginSession = LoginSession()
    private var availableProjects: List<ManagerProject> = emptyList()

    private val contentLayout: LinearLayout
        get() = ((binding.materialHomeRoot.getChildAt(0) as NestedScrollView).getChildAt(0) as LinearLayout)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMaterialHomeBinding.inflate(layoutInflater)
        sessionStore = SessionStore(applicationContext)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.materialHomeRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        bindActions()
        renderLoading()
        loadData()
    }

    private fun bindActions() {
        binding.titleText.text = "材料资源"
        binding.projectSwitchText.text = "请选择项目"
        binding.backButton.setOnClickListener { finish() }
        binding.searchButton.setOnClickListener { openMaterialList() }
        binding.notificationButton.setOnClickListener { toast("暂无新的材料库存预警") }
        binding.projectSwitchText.setOnClickListener { showProjectSwitchDialog() }
        binding.materialListEntry.setOnClickListener { openMaterialList() }
        binding.materialReportEntry.setOnClickListener { openMaterialReport() }
    }

    private fun loadData() {
        lifecycleScope.launch {
            renderLoading()
            runCatching {
                currentSession = sessionStore.sessionFlow.first()
                currentToken = currentSession.accessToken
                currentProjectId = intent.getLongExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, 0L)
                    .takeIf { it > 0L }
                    ?: currentSession.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                binding.projectSwitchText.text = currentSession.projectName.ifBlank { "请选择项目" }
                materialRepository.overview(currentToken, currentProjectId)
            }.onSuccess { overview ->
                renderOverview(overview)
                loadProjectOptions()
            }.onFailure {
                renderError(it.message ?: "材料资源加载失败")
            }
        }
    }

    private fun loadProjectOptions() {
        lifecycleScope.launch {
            runCatching { projectRepository.loadProjects(currentToken) }
                .onSuccess { projects ->
                    availableProjects = projects
                    projects.firstOrNull { it.projectId == currentProjectId }?.let {
                        binding.projectSwitchText.text = it.projectName.ifBlank { "请选择项目" }
                        persistProjectSelectionIfNeeded(it)
                    }
                }
        }
    }

    private fun renderLoading() {
        clearBody()
        contentLayout.addView(messageCard("正在加载材料库存", "从后端读取当前项目的材料档案、库存和低库存预警。"))
    }

    private fun renderError(message: String) {
        clearBody()
        contentLayout.addView(messageCard("材料资源加载失败", "$message\n请确认后端服务、账号项目权限和材料初始化数据。"))
        contentLayout.addView(primaryButton("重新加载") { loadData() })
    }

    private fun renderOverview(overview: MaterialOverview) {
        clearBody()
        contentLayout.addView(sectionTitle("库存风险看板"))
        contentLayout.addView(riskPanel(overview))

        contentLayout.addView(sectionTitle("材料分类"))
        if (overview.categories.isEmpty()) {
            contentLayout.addView(messageCard("暂无材料分类", "当前项目还没有材料库存数据，请先在后台维护材料档案、仓库堆场和当前库存。"))
        } else {
            overview.categories.chunked(2).forEach { rowItems ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        leftMargin = 16.dp()
                        rightMargin = 16.dp()
                    }
                }
                rowItems.forEachIndexed { index, category ->
                    row.addView(categoryCard(category), LinearLayout.LayoutParams(0, 96.dp(), 1f).apply {
                        if (index > 0) leftMargin = 10.dp()
                        topMargin = 10.dp()
                    })
                }
                if (rowItems.size == 1) {
                    row.addView(View(this), LinearLayout.LayoutParams(0, 96.dp(), 1f).apply { leftMargin = 10.dp() })
                }
                contentLayout.addView(row)
            }
        }

        contentLayout.addView(primaryButton("查看全部库存") { openMaterialList() })
        contentLayout.addView(outlineButton("只看低库存") { openMaterialList(filter = MaterialListActivity.FILTER_LOW) })
        contentLayout.addView(outlineButton("资源报表") { openMaterialReport() })
    }

    private fun clearBody() {
        while (contentLayout.childCount > 1) contentLayout.removeViewAt(1)
    }

    private fun riskPanel(overview: MaterialOverview): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.bg_card_stroke)
            setPadding(16.dp(), 14.dp(), 16.dp(), 14.dp())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                leftMargin = 16.dp()
                rightMargin = 16.dp()
                topMargin = 10.dp()
            }

            addView(TextView(context).apply {
                text = "现场领用优先看风险，低库存材料需要及时补库。"
                setTextColor(Color.parseColor("#4B5563"))
                textSize = 13f
            })

            addView(TextView(context).apply {
                text = "${overview.lowStockCount}"
                setTextColor(if (overview.lowStockCount > 0) Color.parseColor("#DC2626") else Color.parseColor("#059669"))
                textSize = 36f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, 12.dp(), 0, 0)
            })
            addView(TextView(context).apply {
                text = "低库存材料"
                setTextColor(Color.parseColor("#111827"))
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            })

            addView(metricRow(
                "材料种类",
                "${overview.materialCount} 种",
                "库存总量",
                overview.stockTotal.formatQty(),
            ), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 76.dp()).apply { topMargin = 14.dp() })

            setOnClickListener { openMaterialList(filter = MaterialListActivity.FILTER_LOW) }
        }
    }

    private fun metricRow(leftTitle: String, leftValue: String, rightTitle: String, rightValue: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(metricBox(leftTitle, leftValue), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { rightMargin = 6.dp() })
            addView(metricBox(rightTitle, rightValue), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { leftMargin = 6.dp() })
        }
    }

    private fun metricBox(title: String, value: String): TextView {
        return TextView(this).apply {
            background = getDrawable(R.drawable.bg_search_pill)
            gravity = Gravity.CENTER
            setPadding(10.dp(), 0, 10.dp(), 0)
            text = "$title\n$value"
            setTextColor(Color.parseColor("#111827"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setLineSpacing(4.dp().toFloat(), 1.0f)
        }
    }

    private fun categoryCard(category: MaterialCategory): TextView {
        val unitText = category.unit.ifBlank { "单位" }
        val categoryName = category.category.ifBlank { "未分类" }
        return TextView(this).apply {
            background = getDrawable(R.drawable.bg_card_stroke)
            gravity = Gravity.CENTER
            setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())
            text = "$categoryName\n${category.quantity.formatQty()} $unitText\n点击查看"
            setTextColor(Color.parseColor("#1F2937"))
            textSize = 14f
            setLineSpacing(4.dp().toFloat(), 1.0f)
            setOnClickListener { openMaterialList(category = categoryName) }
        }
    }

    private fun sectionTitle(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            setTextColor(Color.parseColor("#111827"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                leftMargin = 16.dp()
                rightMargin = 16.dp()
                topMargin = 18.dp()
            }
        }
    }

    private fun messageCard(title: String, body: String): TextView {
        return TextView(this).apply {
            background = getDrawable(R.drawable.bg_card_stroke)
            setPadding(16.dp(), 14.dp(), 16.dp(), 14.dp())
            text = "$title\n$body"
            setTextColor(Color.parseColor("#374151"))
            textSize = 14f
            setLineSpacing(6.dp().toFloat(), 1.0f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                leftMargin = 16.dp()
                rightMargin = 16.dp()
                topMargin = 16.dp()
            }
        }
    }

    private fun primaryButton(textValue: String, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = textValue
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = getDrawable(R.drawable.bg_round_blue)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 48.dp()).apply {
                leftMargin = 16.dp()
                rightMargin = 16.dp()
                topMargin = 18.dp()
            }
        }
    }

    private fun outlineButton(textValue: String, action: () -> Unit): TextView {
        return primaryButton(textValue, action).apply {
            setTextColor(Color.parseColor("#1677FF"))
            background = getDrawable(R.drawable.bg_outline_blue)
            (layoutParams as LinearLayout.LayoutParams).topMargin = 10.dp()
        }
    }

    private fun showProjectSwitchDialog() {
        if (availableProjects.isEmpty()) {
            toast("项目列表加载中，请稍后再试")
            loadProjectOptions()
            return
        }
        val names = availableProjects.map { it.projectName.ifBlank { "未命名项目" } }.toTypedArray()
        val checkedIndex = availableProjects.indexOfFirst { it.projectId == currentProjectId }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("切换项目")
            .setSingleChoiceItems(names, checkedIndex) { dialog, which ->
                dialog.dismiss()
                switchProject(availableProjects[which])
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun switchProject(project: ManagerProject) {
        if (project.projectId == currentProjectId) return
        currentProjectId = project.projectId
        binding.projectSwitchText.text = project.projectName.ifBlank { "请选择项目" }
        persistProjectSelectionIfNeeded(project)
        loadData()
    }

    private fun persistProjectSelectionIfNeeded(project: ManagerProject) {
        if (currentSession.projectId == project.projectId.toString() && currentSession.projectName == project.projectName) return
        currentSession = currentSession.copy(
            projectId = project.projectId.toString(),
            projectName = project.projectName,
            organizationName = project.projectName.ifBlank { currentSession.organizationName },
        )
        lifecycleScope.launch { sessionStore.saveSession(currentSession) }
    }

    private fun openMaterialList(category: String? = null, filter: String = MaterialListActivity.FILTER_ALL) {
        startActivity(
            Intent(this, MaterialListActivity::class.java)
                .putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, currentProjectId)
                .putExtra(MaterialListActivity.EXTRA_CATEGORY, category)
                .putExtra(MaterialListActivity.EXTRA_FILTER, filter),
        )
    }

    private fun openMaterialReport() {
        startActivity(Intent(this, MaterialReportActivity::class.java).putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, currentProjectId))
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Double.formatQty(): String = if (this % 1.0 == 0.0) toLong().toString() else String.format("%.2f", this)

    companion object {
        const val EXTRA_MATERIAL_ID = "materialId"
    }
}
