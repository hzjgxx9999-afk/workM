package com.qkzc.workerm.ui.material

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.R
import com.qkzc.workerm.data.material.MaterialInventoryItem
import com.qkzc.workerm.data.material.MaterialRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityMaterialListBinding
import com.qkzc.workerm.ui.project.ProjectDetailActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MaterialListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaterialListBinding
    private val repository = MaterialRepository()

    private var currentProjectId: Long = 0L
    private var currentToken: String = ""
    private var selectedFilter: String = FILTER_ALL
    private var selectedCategory: String? = null
    private var keyword: String = ""
    private var allRows: List<MaterialInventoryItem> = emptyList()
    private val filterChips = linkedMapOf<String, TextView>()

    private lateinit var searchInput: EditText
    private lateinit var summaryText: TextView
    private lateinit var listContainer: LinearLayout

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

        selectedFilter = intent.getStringExtra(EXTRA_FILTER).takeUnless { it.isNullOrBlank() } ?: FILTER_ALL
        selectedCategory = intent.getStringExtra(EXTRA_CATEGORY)?.takeIf { it.isNotBlank() }

        ((contentLayout.getChildAt(0) as? ViewGroup)?.getChildAt(1) as? TextView)?.text = "材料列表"
        binding.backButton.setOnClickListener { finish() }
        rebuildPageShell()
        renderLoading()
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            runCatching {
                val session = SessionStore(applicationContext).sessionFlow.first()
                currentToken = session.accessToken
                currentProjectId = intent.getLongExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, 0L)
                    .takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                repository.list(
                    token = currentToken,
                    projectId = currentProjectId,
                    pageSize = 200,
                )
            }.onSuccess { rows ->
                allRows = rows
                applyFilters()
            }.onFailure { renderError(it.message ?: "材料列表加载失败") }
        }
    }

    private fun rebuildPageShell() {
        while (contentLayout.childCount > 1) contentLayout.removeViewAt(1)

        contentLayout.addView(searchBar())
        contentLayout.addView(filterRow())
        summaryText = TextView(this).apply {
            setTextColor(Color.parseColor("#6B7280"))
            textSize = 13f
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 34.dp()).apply {
                topMargin = 8.dp()
            }
        }
        contentLayout.addView(summaryText)

        selectedCategory?.let {
            contentLayout.addView(categoryHint(it))
        }

        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        contentLayout.addView(listContainer)
        refreshChipState()
    }

    private fun searchBar(): LinearLayout {
        searchInput = EditText(this).apply {
            background = getDrawable(R.drawable.bg_search_pill)
            hint = "搜索材料名称 / 规格型号 / 编码 / 负责人"
            setHintTextColor(Color.parseColor("#9AA4B2"))
            setTextColor(Color.parseColor("#111827"))
            textSize = 13f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setPadding(14.dp(), 0, 14.dp(), 0)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    keyword = text.toString().trim()
                    applyFilters()
                    true
                } else {
                    false
                }
            }
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 40.dp()).apply {
                topMargin = 8.dp()
            }
            addView(searchInput, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
            addView(TextView(context).apply {
                text = "搜索"
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                background = getDrawable(R.drawable.bg_round_blue)
                setOnClickListener {
                    keyword = searchInput.text.toString().trim()
                    applyFilters()
                }
            }, LinearLayout.LayoutParams(64.dp(), ViewGroup.LayoutParams.MATCH_PARENT).apply { leftMargin = 8.dp() })
        }
    }

    private fun filterRow(): LinearLayout {
        filterChips.clear()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 42.dp()).apply {
                topMargin = 10.dp()
            }
            listOf(
                FILTER_ALL to "全部",
                FILTER_MATERIAL to "材料",
                FILTER_EQUIPMENT to "设备",
                FILTER_LOW to "低库存",
            ).forEachIndexed { index, pair ->
                val chip = TextView(context).apply {
                    text = pair.second
                    gravity = Gravity.CENTER
                    textSize = 14f
                    setOnClickListener {
                        selectedFilter = pair.first
                        refreshChipState()
                        applyFilters()
                    }
                }
                filterChips[pair.first] = chip
                addView(chip, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (index > 0) leftMargin = 8.dp()
                })
            }
        }
    }

    private fun refreshChipState() {
        filterChips.forEach { (filter, chip) ->
            val selected = filter == selectedFilter
            chip.background = getDrawable(if (selected) R.drawable.bg_round_blue else R.drawable.bg_search_pill)
            chip.setTextColor(if (selected) Color.WHITE else Color.parseColor("#1F2937"))
            chip.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private fun applyFilters() {
        val filtered = allRows.filter { item ->
            val categoryMatch = selectedCategory?.let { item.category == it } ?: true
            val filterMatch = when (selectedFilter) {
                FILTER_MATERIAL -> !item.isEquipment()
                FILTER_EQUIPMENT -> item.isEquipment()
                FILTER_LOW -> item.lowStock
                else -> true
            }
            val keywordMatch = keyword.isBlank() || item.matchesKeyword(keyword)
            categoryMatch && filterMatch && keywordMatch
        }
        renderRows(filtered)
    }

    private fun renderLoading() {
        listContainer.removeAllViews()
        summaryText.text = "正在加载材料明细..."
        listContainer.addView(messageCard("正在加载材料明细", "从后端读取当前项目的材料库存列表。"))
    }

    private fun renderError(message: String) {
        listContainer.removeAllViews()
        summaryText.text = "加载失败"
        listContainer.addView(messageCard("加载失败", "$message\n请检查后端接口、Token 和项目权限。"))
    }

    private fun renderRows(rows: List<MaterialInventoryItem>) {
        listContainer.removeAllViews()
        val lowCount = rows.count { it.lowStock }
        val categoryText = selectedCategory?.let { " | 分类：$it" }.orEmpty()
        summaryText.text = "共 ${rows.size} 种 | 低库存 $lowCount 种$categoryText"

        if (rows.isEmpty()) {
            listContainer.addView(messageCard("暂无匹配材料", "当前筛选条件下没有库存数据，可切换分类、筛选项或清空搜索关键词。"))
            return
        }
        rows.forEach { listContainer.addView(materialCard(it)) }
    }

    private fun categoryHint(category: String): TextView {
        return TextView(this).apply {
            text = "当前分类：$category    点击查看全部"
            setTextColor(Color.parseColor("#1677FF"))
            textSize = 13f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12.dp(), 0, 12.dp(), 0)
            background = getDrawable(R.drawable.bg_search_pill)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 34.dp()).apply {
                topMargin = 4.dp()
            }
            setOnClickListener {
                selectedCategory = null
                rebuildPageShell()
                applyFilters()
            }
        }
    }

    private fun materialCard(item: MaterialInventoryItem): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.bg_card_stroke)
            setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 10.dp()
            }
            addView(row(item.itemName.ifBlank { "未命名材料" }, if (item.lowStock) "低库存" else "正常", item.lowStock))
            addView(info("分类：${item.category.ifBlank { "-" }}    编码：${item.materialCode.ifBlank { "-" }}"))
            addView(info("规格型号：${item.specModel.ifBlank { "-" }}"))
            addView(info("当前库存：${item.currentQty.formatQty()} ${item.unit.ifBlank { "单位" }}    安全库存：${item.safeStock.formatQty()}"))
            addView(info("存放位置：${item.locationText.ifBlank { item.warehouseName.ifBlank { "-" } }}"))
            addView(info("负责人：${item.managerName.ifBlank { "-" }}"))
            setOnClickListener {
                startActivity(
                    Intent(this@MaterialListActivity, MaterialDetailActivity::class.java)
                        .putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, currentProjectId)
                        .putExtra(MaterialHomeActivity.EXTRA_MATERIAL_ID, item.id),
                )
            }
        }
    }

    private fun row(title: String, status: String, danger: Boolean): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.parseColor("#111827"))
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(TextView(context).apply {
                text = status
                gravity = Gravity.CENTER
                setTextColor(if (danger) Color.parseColor("#DC2626") else Color.parseColor("#059669"))
                background = getDrawable(if (danger) R.drawable.bg_chip_red_soft else R.drawable.bg_status_soft_green)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }, LinearLayout.LayoutParams(64.dp(), 26.dp()))
        }
    }

    private fun info(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            setTextColor(Color.parseColor("#4B5563"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 6.dp()
            }
        }
    }

    private fun messageCard(title: String, body: String): TextView {
        return TextView(this).apply {
            background = getDrawable(R.drawable.bg_card_stroke)
            setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
            text = "$title\n$body"
            setTextColor(Color.parseColor("#374151"))
            textSize = 14f
            setLineSpacing(6.dp().toFloat(), 1.0f)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 12.dp()
            }
        }
    }

    private fun MaterialInventoryItem.matchesKeyword(value: String): Boolean {
        val query = value.trim()
        return listOf(itemName, specModel, materialCode, category, warehouseName, locationText, managerName)
            .any { it.contains(query, ignoreCase = true) }
    }

    private fun MaterialInventoryItem.isEquipment(): Boolean {
        val text = "$category $itemName $materialCode"
        return listOf("设备", "机械", "挖掘机", "塔吊", "泵车", "EQUIP")
            .any { text.contains(it, ignoreCase = true) }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Double.formatQty(): String = if (this % 1.0 == 0.0) toLong().toString() else String.format("%.2f", this)

    companion object {
        const val EXTRA_CATEGORY = "extra_material_category"
        const val EXTRA_FILTER = "extra_material_filter"
        const val FILTER_ALL = "all"
        const val FILTER_MATERIAL = "material"
        const val FILTER_EQUIPMENT = "equipment"
        const val FILTER_LOW = "low"
    }
}
