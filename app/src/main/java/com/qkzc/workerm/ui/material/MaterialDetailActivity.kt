package com.qkzc.workerm.ui.material

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.R
import com.qkzc.workerm.data.material.MaterialInventoryItem
import com.qkzc.workerm.data.material.MaterialRepository
import com.qkzc.workerm.data.material.MaterialStockRecord
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityMaterialDetailBinding
import com.qkzc.workerm.ui.project.ProjectDetailActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MaterialDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaterialDetailBinding
    private val repository = MaterialRepository()

    private var currentProjectId: Long = 0L
    private var currentMaterialId: Long = 0L
    private var currentToken: String = ""
    private var currentItem: MaterialInventoryItem? = null
    private var warehouseOptions: List<WarehouseOption> = emptyList()

    private val contentLayout: LinearLayout
        get() = (binding.materialDetailRoot.getChildAt(0) as ViewGroup).getChildAt(0) as LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMaterialDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.materialDetailRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }

        ((contentLayout.getChildAt(0) as? ViewGroup)?.getChildAt(1) as? TextView)?.text = "材料详情"
        binding.backButton.setOnClickListener { finish() }
        currentProjectId = intent.getLongExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, 0L)
        currentMaterialId = intent.getLongExtra(MaterialHomeActivity.EXTRA_MATERIAL_ID, 0L)
        bindBottomButtons()
        renderLoading()
        loadData()
    }

    private fun bindBottomButtons() {
        val buttons = binding.materialDetailRoot.allTextViews().takeLast(2)
        buttons.getOrNull(0)?.apply {
            text = "入库登记"
            setOnClickListener { showChangeDialog(inbound = true) }
        }
        buttons.getOrNull(1)?.apply {
            text = "出库登记"
            setOnClickListener { showChangeDialog(inbound = false) }
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            runCatching {
                val session = SessionStore(applicationContext).sessionFlow.first()
                currentToken = session.accessToken
                currentProjectId = currentProjectId.takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                if (currentMaterialId <= 0L) {
                    currentMaterialId = repository.list(currentToken, currentProjectId, pageSize = 1).firstOrNull()?.id
                        ?: error("当前项目暂无材料库存")
                }

                val detail = repository.detail(currentToken, currentProjectId, currentMaterialId)
                val records = repository.records(currentToken, currentProjectId, currentMaterialId).take(8)
                val materialStocks = repository.list(currentToken, currentProjectId, pageSize = 200)
                    .filter { it.id == currentMaterialId && it.warehouseId > 0L }
                val options = materialStocks.toWarehouseOptions(detail)
                Triple(detail, records, options)
            }.onSuccess { (detail, records, options) ->
                currentItem = detail
                warehouseOptions = options
                render(detail, records)
            }.onFailure { renderError(it.message ?: "材料详情加载失败") }
        }
    }

    private fun renderLoading() {
        clearBody()
        contentLayout.addView(messageCard("正在加载材料详情", "读取材料库存、仓库信息和最近出入库流水。"))
    }

    private fun renderError(message: String) {
        clearBody()
        contentLayout.addView(messageCard("加载失败", "$message\n请确认项目材料数据和接口权限。"))
    }

    private fun render(item: MaterialInventoryItem, records: List<MaterialStockRecord>) {
        clearBody()
        contentLayout.addView(headerCard(item))
        contentLayout.addView(stockPanel(item))
        contentLayout.addView(infoCard("存放信息", listOf(
            "仓库：${item.warehouseName.ifBlank { "-" }}",
            "位置：${item.locationText.ifBlank { "-" }}",
            "负责人：${item.managerName.ifBlank { "-" }}",
            "联系电话：${item.contactPhone.ifBlank { "-" }}",
        )))
        contentLayout.addView(recordCard(records, item.unit))
    }

    private fun clearBody() {
        while (contentLayout.childCount > 1) contentLayout.removeViewAt(1)
    }

    private fun headerCard(item: MaterialInventoryItem): LinearLayout {
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
            addView(titleRow(item.itemName.ifBlank { "未命名材料" }, if (item.lowStock) "低库存" else "正常", item.lowStock))
            addView(infoText("分类：${item.category.ifBlank { "-" }}    规格：${item.specModel.ifBlank { "-" }}"))
            addView(infoText("单位：${item.unit.ifBlank { "-" }}    编码：${item.materialCode.ifBlank { "-" }}"))
        }
    }

    private fun stockPanel(item: MaterialInventoryItem): LinearLayout {
        val unit = item.unit.ifBlank { "单位" }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.bg_card_stroke)
            setPadding(16.dp(), 14.dp(), 16.dp(), 14.dp())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 12.dp()
            }

            addView(titleRow("当前库存", if (item.lowStock) "需补库" else "安全", item.lowStock))
            addView(TextView(context).apply {
                text = "${item.currentQty.formatQty()} $unit"
                setTextColor(if (item.lowStock) Color.parseColor("#DC2626") else Color.parseColor("#111827"))
                textSize = 34f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 12.dp(), 0, 4.dp())
            })
            addView(TextView(context).apply {
                text = if (item.lowStock) {
                    "当前库存已低于或等于安全库存，请优先安排补库。"
                } else {
                    "库存处于安全线以上，可继续按现场领用登记出库。"
                }
                setTextColor(if (item.lowStock) Color.parseColor("#B91C1C") else Color.parseColor("#047857"))
                textSize = 13f
            })
            addView(metricRow(
                "安全库存",
                "${item.safeStock.formatQty()} $unit",
                "最高库存",
                item.maxStock.formatLimitQty(unit),
                "锁定库存",
                "${item.lockedQty.formatQty()} $unit",
            ))
        }
    }

    private fun metricRow(a: String, av: String, b: String, bv: String, c: String, cv: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 56.dp()).apply {
                topMargin = 14.dp()
            }
            addView(metric("$a\n$av"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
            addView(metric("$b\n$bv"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { leftMargin = 8.dp() })
            addView(metric("$c\n$cv"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { leftMargin = 8.dp() })
        }
    }

    private fun metric(textValue: String): TextView = TextView(this).apply {
        text = textValue
        background = getDrawable(R.drawable.bg_search_pill)
        gravity = Gravity.CENTER
        setTextColor(Color.parseColor("#1F2937"))
        textSize = 12f
        setLineSpacing(3.dp().toFloat(), 1.0f)
    }

    private fun infoCard(title: String, lines: List<String>): TextView {
        return messageCard(title, lines.joinToString("\n"))
    }

    private fun recordCard(records: List<MaterialStockRecord>, unit: String): TextView {
        val body = if (records.isEmpty()) {
            "暂无出入库流水"
        } else {
            records.joinToString("\n") {
                "${formatBizType(it.bizType)}  ${it.changeQty.formatQty()} ${unit.ifBlank { it.unit }}  ${it.createTime.ifBlank { "-" }}  ${it.operatorName.ifBlank { "-" }}"
            }
        }
        return messageCard("最近出入库流水", body)
    }

    private fun titleRow(title: String, status: String, danger: Boolean): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.parseColor("#111827"))
                textSize = 18f
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

    private fun infoText(textValue: String): TextView = TextView(this).apply {
        text = textValue
        setTextColor(Color.parseColor("#4B5563"))
        textSize = 13f
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 6.dp()
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

    private fun showChangeDialog(inbound: Boolean) {
        val item = currentItem ?: run {
            Toast.makeText(this, "材料数据未加载完成", Toast.LENGTH_SHORT).show()
            return
        }
        if (warehouseOptions.isEmpty()) {
            Toast.makeText(this, "当前材料没有可选择的仓库，请先在后台维护库存仓库", Toast.LENGTH_SHORT).show()
            return
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 8.dp(), 24.dp(), 0)
        }
        val warehouseSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MaterialDetailActivity,
                android.R.layout.simple_spinner_dropdown_item,
                warehouseOptions.map { it.displayName },
            )
            setSelection(warehouseOptions.indexOfFirst { it.warehouseId == item.warehouseId }.coerceAtLeast(0))
        }
        val quantityInput = EditText(this).apply {
            hint = "请输入${if (inbound) "入库" else "出库"}数量"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = getDrawable(R.drawable.bg_audit_input)
            setPadding(12.dp(), 0, 12.dp(), 0)
        }
        val remarkInput = EditText(this).apply {
            hint = if (inbound) "来源/备注，例如：采购入库" else "用途/备注，例如：现场领用"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            background = getDrawable(R.drawable.bg_audit_input)
            setPadding(12.dp(), 8.dp(), 12.dp(), 8.dp())
        }

        container.addView(dialogLabel("选择仓库"))
        container.addView(warehouseSpinner)
        container.addView(dialogLabel("数量"))
        container.addView(quantityInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 46.dp()))
        container.addView(dialogLabel("备注"))
        container.addView(remarkInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 74.dp()))

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (inbound) "入库登记" else "出库登记")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val warehouse = warehouseOptions[warehouseSpinner.selectedItemPosition]
                val quantity = quantityInput.text.toString().trim().toDoubleOrNull()
                if (quantity == null || quantity <= 0.0) {
                    Toast.makeText(this, "请输入有效数量", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!inbound && quantity > warehouse.currentQty) {
                    Toast.makeText(this, "出库数量不能超过所选仓库当前库存", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dialog.dismiss()
                submitInventoryChange(inbound, item, warehouse.warehouseId, quantity, remarkInput.text.toString())
            }
        }
        dialog.show()
    }

    private fun submitInventoryChange(
        inbound: Boolean,
        item: MaterialInventoryItem,
        warehouseId: Long,
        quantity: Double,
        remark: String,
    ) {
        lifecycleScope.launch {
            runCatching {
                if (inbound) {
                    repository.inbound(currentToken, currentProjectId, item.id, warehouseId, quantity, remark)
                } else {
                    repository.outbound(currentToken, currentProjectId, item.id, warehouseId, quantity, remark)
                }
            }.onSuccess {
                Toast.makeText(this@MaterialDetailActivity, "登记成功", Toast.LENGTH_SHORT).show()
                loadData()
            }.onFailure {
                Toast.makeText(this@MaterialDetailActivity, it.message ?: "登记失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dialogLabel(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            setTextColor(Color.parseColor("#374151"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 12.dp()
                bottomMargin = 6.dp()
            }
        }
    }

    private fun List<MaterialInventoryItem>.toWarehouseOptions(fallback: MaterialInventoryItem): List<WarehouseOption> {
        val options = distinctBy { it.warehouseId }.map {
            WarehouseOption(
                warehouseId = it.warehouseId,
                displayName = "${it.warehouseName.ifBlank { "仓库${it.warehouseId}" }} | ${it.locationText.ifBlank { "未填写位置" }} | 库存 ${it.currentQty.formatQty()} ${it.unit.ifBlank { fallback.unit }}",
                currentQty = it.currentQty,
            )
        }
        if (options.isNotEmpty()) return options
        return fallback.warehouseId.takeIf { it > 0L }?.let {
            listOf(
                WarehouseOption(
                    warehouseId = it,
                    displayName = "${fallback.warehouseName.ifBlank { "仓库$it" }} | ${fallback.locationText.ifBlank { "未填写位置" }} | 库存 ${fallback.currentQty.formatQty()} ${fallback.unit.ifBlank { "单位" }}",
                    currentQty = fallback.currentQty,
                ),
            )
        }.orEmpty()
    }

    private fun formatBizType(type: String): String = when (type) {
        "IN" -> "入库"
        "OUT" -> "出库"
        "RETURN" -> "退库"
        "ADJUST" -> "调整"
        "TRANSFER" -> "调拨"
        else -> type.ifBlank { "-" }
    }

    private fun android.view.View.allTextViews(): List<TextView> {
        val out = mutableListOf<TextView>()
        fun visit(view: android.view.View) {
            if (view is TextView) out += view
            if (view is android.view.ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
        }
        visit(this)
        return out
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Double.formatQty(): String = if (this % 1.0 == 0.0) toLong().toString() else String.format("%.2f", this)
    private fun Double.formatLimitQty(unit: String): String = if (this <= 0.0) "-" else "${formatQty()} $unit"

    private data class WarehouseOption(
        val warehouseId: Long,
        val displayName: String,
        val currentQty: Double,
    )
}
