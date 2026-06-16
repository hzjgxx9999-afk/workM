package com.qkzc.workerm.ui.material

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.R
import com.qkzc.workerm.data.material.MaterialReport
import com.qkzc.workerm.data.material.MaterialRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityMaterialReportBinding
import com.qkzc.workerm.ui.project.ProjectDetailActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MaterialReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaterialReportBinding
    private val repository = MaterialRepository()
    private var currentProjectId: Long = 0L

    private val contentLayout: LinearLayout
        get() = binding.materialReportRoot.getChildAt(0) as LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMaterialReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.materialReportRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        currentProjectId = intent.getLongExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, 0L)
        renderLoading()
        loadReport()
    }

    private fun loadReport() {
        lifecycleScope.launch {
            runCatching {
                val session = SessionStore(applicationContext).sessionFlow.first()
                val projectId = currentProjectId.takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                currentProjectId = projectId
                repository.report(session.accessToken, projectId)
            }.onSuccess(::renderReport)
                .onFailure { renderError(it.message ?: "资源报表加载失败") }
        }
    }

    private fun renderLoading() {
        clearBody()
        contentLayout.addView(messageCard("正在加载资源报表...", "读取材料统计、分类占比和出入库流水。"))
    }

    private fun renderError(message: String) {
        clearBody()
        contentLayout.addView(messageCard("加载失败", "$message\n请确认接口权限和材料初始化数据。"))
    }

    private fun renderReport(report: MaterialReport) {
        clearBody()
        contentLayout.addView(sectionTitle("关键指标"))
        contentLayout.addView(metricGrid(report))
        contentLayout.addView(sectionTitle("材料库存结构"))
        contentLayout.addView(messageCard("分类占比", report.categories.take(8).joinToString("\n") {
            "${it.category.ifBlank { "未分类" }}    ${it.quantity.formatQty()} ${it.unit.ifBlank { "单位" }}"
        }.ifBlank { "暂无分类库存数据" }))
        contentLayout.addView(sectionTitle("最近出入库"))
        contentLayout.addView(messageCard("流水摘要", report.records.take(10).joinToString("\n") {
            "${formatBizType(it.bizType)}  ${it.itemName.ifBlank { "-" }}  ${it.changeQty.formatQty()} ${it.unit.ifBlank { "单位" }}  ${it.createTime.ifBlank { "-" }}"
        }.ifBlank { "暂无出入库流水" }))
        contentLayout.addView(messageCard("报表导出", "移动端展示统计摘要；Excel/PDF 导出请在 Web 管理后台“资源管理”中操作。"))
    }

    private fun clearBody() {
        while (contentLayout.childCount > 2) contentLayout.removeViewAt(2)
    }

    private fun metricGrid(report: MaterialReport): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 8.dp()
            }
            addView(metricRow("材料种类", "${report.materialCount} 种", "库存总量", report.stockTotal.formatQty()))
            addView(metricRow("低库存", "${report.lowStockCount} 种", "项目ID", currentProjectId.toString()), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 82.dp()).apply {
                topMargin = 8.dp()
            })
        }
    }

    private fun metricRow(leftTitle: String, leftValue: String, rightTitle: String, rightValue: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(metricCard(leftTitle, leftValue), LinearLayout.LayoutParams(0, 82.dp(), 1f).apply { rightMargin = 6.dp() })
            addView(metricCard(rightTitle, rightValue), LinearLayout.LayoutParams(0, 82.dp(), 1f).apply { leftMargin = 6.dp() })
        }
    }

    private fun metricCard(title: String, value: String): TextView = TextView(this).apply {
        background = getDrawable(R.drawable.bg_card_stroke)
        gravity = android.view.Gravity.CENTER
        text = "$title\n$value"
        setTextColor(Color.parseColor("#111827"))
        textSize = 15f
        setLineSpacing(4.dp().toFloat(), 1.0f)
    }

    private fun sectionTitle(textValue: String): TextView = TextView(this).apply {
        text = textValue
        setTextColor(Color.parseColor("#111827"))
        textSize = 16f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 14.dp()
        }
    }

    private fun messageCard(title: String, body: String): TextView = TextView(this).apply {
        background = getDrawable(R.drawable.bg_card_stroke)
        setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
        text = "$title\n$body"
        setTextColor(Color.parseColor("#374151"))
        textSize = 14f
        setLineSpacing(6.dp().toFloat(), 1.0f)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 10.dp()
        }
    }

    private fun formatBizType(type: String): String = when (type) {
        "IN" -> "入库"
        "OUT" -> "出库"
        "RETURN" -> "退库"
        "ADJUST" -> "调整"
        "TRANSFER" -> "调拨"
        else -> type.ifBlank { "-" }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Double.formatQty(): String = if (this % 1.0 == 0.0) toLong().toString() else String.format("%.2f", this)
}
