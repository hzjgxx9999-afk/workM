package com.qkzc.workerm.ui.material

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
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.R
import com.qkzc.workerm.data.material.MaterialRepository
import com.qkzc.workerm.data.material.MaterialStockWarning
import com.qkzc.workerm.data.material.MaterialWarningSummary
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityMaterialListBinding
import com.qkzc.workerm.ui.project.ProjectDetailActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MaterialWarningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaterialListBinding
    private lateinit var summaryText: TextView
    private lateinit var listContainer: LinearLayout

    private val repository = MaterialRepository()
    private var currentProjectId: Long = 0L
    private var currentToken: String = ""

    private val contentLayout: LinearLayout
        get() = binding.materialListRoot.getChildAt(0) as LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMaterialListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.materialListRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        ((contentLayout.getChildAt(0) as? ViewGroup)?.getChildAt(1) as? TextView)?.text = "库存预警"
        binding.backButton.setOnClickListener { finish() }
        rebuildPageShell()
        renderLoading()
        loadData()
    }

    private fun rebuildPageShell() {
        while (contentLayout.childCount > 1) contentLayout.removeViewAt(1)
        summaryText = TextView(this).apply {
            setTextColor(Color.parseColor("#4B5563"))
            textSize = 13f
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 42.dp()).apply {
                topMargin = 8.dp()
            }
        }
        contentLayout.addView(summaryText)
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        contentLayout.addView(listContainer)
    }

    private fun loadData() {
        lifecycleScope.launch {
            renderLoading()
            runCatching {
                val session = SessionStore(applicationContext).sessionFlow.first()
                currentToken = session.accessToken
                currentProjectId = intent.getLongExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, 0L)
                    .takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                repository.warningSummary(currentToken, currentProjectId) to
                    repository.warningPage(currentToken, currentProjectId)
            }.onSuccess { (summary, warnings) ->
                renderWarnings(summary, warnings)
            }.onFailure {
                renderError(it.message ?: "材料库存预警加载失败")
            }
        }
    }

    private fun renderLoading() {
        summaryText.text = "正在加载库存预警..."
        listContainer.removeAllViews()
        listContainer.addView(messageCard("正在加载库存预警", "从后端读取当前项目的材料低库存预警。"))
    }

    private fun renderError(message: String) {
        summaryText.text = "加载失败"
        listContainer.removeAllViews()
        listContainer.addView(messageCard("库存预警加载失败", "$message\n请检查后端服务、Token 和项目权限。"))
        listContainer.addView(primaryButton("重新加载") { loadData() })
    }

    private fun renderWarnings(summary: MaterialWarningSummary, warnings: List<MaterialStockWarning>) {
        summaryText.text = "活跃 ${summary.openCount} 条 | 严重 ${summary.criticalCount} 条 | 未读 ${summary.unreadCount} 条"
        listContainer.removeAllViews()
        if (warnings.isEmpty()) {
            listContainer.addView(messageCard("暂无材料库存预警", "当前项目没有待确认的材料低库存预警。"))
            return
        }
        warnings.forEach { warning ->
            listContainer.addView(warningCard(warning))
        }
    }

    private fun warningCard(warning: MaterialStockWarning): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.bg_card_stroke)
            setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 10.dp()
            }

            addView(headerRow(warning))
            addView(TextView(context).apply {
                text = "当前可用 ${warning.availableQty.formatQty()}${warning.unit}，安全库存 ${warning.safeStock.formatQty()}${warning.unit}，缺口 ${warning.shortageQty.formatQty()}${warning.unit}"
                setTextColor(Color.parseColor("#374151"))
                textSize = 13f
                setPadding(0, 10.dp(), 0, 0)
            })
            addView(TextView(context).apply {
                text = "最后触发：${warning.lastTriggerTime.ifBlank { warning.firstTriggerTime.ifBlank { "-" } }}"
                setTextColor(Color.parseColor("#6B7280"))
                textSize = 12f
                setPadding(0, 6.dp(), 0, 0)
            })
            if (warning.status != "RESOLVED") {
                addView(primaryButton(if (warning.status == "ACKNOWLEDGED") "已确认，刷新状态" else "确认预警") {
                    acknowledge(warning)
                })
            }
        }
    }

    private fun headerRow(warning: MaterialStockWarning): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply {
                text = warning.itemName.ifBlank { "未命名材料" }
                setTextColor(Color.parseColor("#111827"))
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(TextView(context).apply {
                text = "${levelLabel(warning.warningLevel)} · ${statusLabel(warning.status)}"
                setTextColor(if (warning.warningLevel == "CRITICAL") Color.parseColor("#DC2626") else Color.parseColor("#D97706"))
                textSize = 12f
                gravity = Gravity.CENTER
                background = getDrawable(R.drawable.bg_search_pill)
                setPadding(10.dp(), 4.dp(), 10.dp(), 4.dp())
            })
        }
    }

    private fun acknowledge(warning: MaterialStockWarning) {
        lifecycleScope.launch {
            runCatching {
                repository.ackWarning(currentToken, currentProjectId, warning.warningId)
            }.onSuccess {
                Toast.makeText(this@MaterialWarningActivity, "已确认库存预警", Toast.LENGTH_SHORT).show()
                loadData()
            }.onFailure {
                Toast.makeText(this@MaterialWarningActivity, it.message ?: "确认失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun messageCard(title: String, message: String): TextView {
        return TextView(this).apply {
            text = "$title\n$message"
            setTextColor(Color.parseColor("#4B5563"))
            textSize = 14f
            setLineSpacing(4f, 1f)
            background = getDrawable(R.drawable.bg_card_stroke)
            setPadding(16.dp(), 14.dp(), 16.dp(), 14.dp())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 10.dp()
            }
        }
    }

    private fun primaryButton(textValue: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = textValue
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = getDrawable(R.drawable.bg_round_blue)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                42.dp(),
            ).apply {
                topMargin = 12.dp()
            }
        }
    }

    private fun levelLabel(level: String): String = when (level) {
        "CRITICAL" -> "严重"
        "WARNING" -> "预警"
        else -> "预警"
    }

    private fun statusLabel(status: String): String = when (status) {
        "OPEN" -> "待确认"
        "ACKNOWLEDGED" -> "已确认"
        "RESOLVED" -> "已恢复"
        else -> status.ifBlank { "待确认" }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Double.formatQty(): String = if (this % 1.0 == 0.0) toLong().toString() else String.format("%.2f", this)
}
