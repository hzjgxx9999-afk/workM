package com.qkzc.workerm.ui.message

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.qkzc.workerm.R
import com.qkzc.workerm.data.aiwarning.model.AiWarningHandleStatus
import com.qkzc.workerm.data.aiwarning.model.AiWarningItem
import com.qkzc.workerm.data.aiwarning.model.AiWarningRiskLevel
import com.qkzc.workerm.databinding.ItemAiWarningBinding

class AiWarningAdapter(
    private val onItemClick: (AiWarningItem) -> Unit,
    private val onMarkReadClick: (AiWarningItem) -> Unit,
    private val onHandleClick: (AiWarningItem) -> Unit,
) : RecyclerView.Adapter<AiWarningAdapter.AiWarningViewHolder>() {

    private val items = mutableListOf<AiWarningItem>()

    fun submitList(newItems: List<AiWarningItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AiWarningViewHolder {
        val binding = ItemAiWarningBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return AiWarningViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AiWarningViewHolder, position: Int) {
        holder.bind(items[position], onItemClick, onMarkReadClick, onHandleClick)
    }

    override fun getItemCount(): Int = items.size

    class AiWarningViewHolder(
        private val binding: ItemAiWarningBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: AiWarningItem,
            onItemClick: (AiWarningItem) -> Unit,
            onMarkReadClick: (AiWarningItem) -> Unit,
            onHandleClick: (AiWarningItem) -> Unit,
        ) {
            val context = binding.root.context
            binding.titleText.text = item.title
            binding.contentText.text = item.content.ifBlank { item.summary }
            binding.projectText.text = listOf(item.projectName, item.inspectionPoint)
                .filter(String::isNotBlank)
                .joinToString(separator = "  |  ")
            binding.workerText.text = listOf(item.workerName, item.workType)
                .filter(String::isNotBlank)
                .joinToString(separator = " · ")
            binding.timeText.text = item.createTime
            binding.riskBadgeText.text = item.riskLevelText
            binding.scoreText.text = item.riskScore?.let { "$it 分" } ?: "--"
            binding.statusText.text = item.handleStatusText
            binding.readStatusText.text = if (item.readFlag) "已读" else "未读"

            val (riskTextColor, riskBgColor) = when (item.riskLevel) {
                AiWarningRiskLevel.HIGH -> R.color.danger to R.color.metric_red
                AiWarningRiskLevel.MEDIUM -> R.color.warning to R.color.metric_orange
                else -> R.color.dashboard_blue to R.color.metric_blue
            }
            binding.riskBadgeText.setTextColor(ContextCompat.getColor(context, riskTextColor))
            binding.riskBadgeText.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, riskBgColor))

            val isPending = item.handleStatus == AiWarningHandleStatus.PENDING
            binding.handleButton.isVisible = isPending
            binding.markReadButton.isVisible = !item.readFlag
            binding.root.alpha = if (isPending) 1f else 0.82f

            binding.root.setOnClickListener { onItemClick(item) }
            binding.markReadButton.setOnClickListener { onMarkReadClick(item) }
            binding.handleButton.setOnClickListener { onHandleClick(item) }
        }
    }
}
