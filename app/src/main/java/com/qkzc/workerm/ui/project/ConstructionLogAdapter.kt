package com.qkzc.workerm.ui.project

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.qkzc.workerm.R
import com.qkzc.workerm.data.project.ConstructionLogAttachment
import com.qkzc.workerm.data.project.ConstructionLogItem
import com.qkzc.workerm.databinding.ItemConstructionLogAttachmentBinding
import com.qkzc.workerm.databinding.ItemConstructionLogBinding

class ConstructionLogAdapter(
    private val onItemClick: (ConstructionLogItem) -> Unit,
) : RecyclerView.Adapter<ConstructionLogAdapter.ConstructionLogViewHolder>() {

    private val items = mutableListOf<ConstructionLogItem>()

    fun submitList(newItems: List<ConstructionLogItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConstructionLogViewHolder {
        val binding = ItemConstructionLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ConstructionLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConstructionLogViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ConstructionLogViewHolder(
        private val binding: ItemConstructionLogBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ConstructionLogItem, onItemClick: (ConstructionLogItem) -> Unit) {
            binding.logDateText.text = item.logDate.ifBlank { "--" }
            binding.titleText.text = item.title.ifBlank { "未命名日志" }
            binding.metaText.text = buildString {
                append("班组 ")
                append(item.teamName.ifBlank { "-" })
                append("  ·  提交人 ")
                append(item.recorderName.ifBlank { "-" })
            }
            binding.summaryText.text = buildString {
                append("施工人数 ${item.workerCount} 人")
                append("    机械 ${item.machineCount} 台")
                if (item.currentAuditorName.isNotBlank()) {
                    append("\n当前审核人 ${item.currentAuditorName}")
                }
            }
            binding.timeText.text = item.updateTime.ifBlank { item.submittedAt.ifBlank { "-" } }
            binding.statusText.text = item.statusName.ifBlank { item.status }
            val context = binding.root.context
            val (textColor, bgColor) = when (item.status.uppercase()) {
                "APPROVED", "ARCHIVED" -> R.color.success to R.color.metric_green
                "REJECTED" -> R.color.danger to R.color.metric_red
                "SUBMITTED", "UNDER_REVIEW" -> R.color.warning to R.color.metric_orange
                else -> R.color.dashboard_blue to R.color.metric_blue
            }
            binding.statusText.setTextColor(ContextCompat.getColor(context, textColor))
            binding.statusText.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, bgColor))
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}

class ConstructionLogAttachmentAdapter(
    private val editable: Boolean,
    private val onRemoveClick: ((ConstructionLogAttachment) -> Unit)? = null,
) : RecyclerView.Adapter<ConstructionLogAttachmentAdapter.AttachmentViewHolder>() {

    private val items = mutableListOf<ConstructionLogAttachment>()

    fun submitList(newItems: List<ConstructionLogAttachment>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttachmentViewHolder {
        val binding = ItemConstructionLogAttachmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return AttachmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttachmentViewHolder, position: Int) {
        holder.bind(items[position], editable, onRemoveClick)
    }

    override fun getItemCount(): Int = items.size

    class AttachmentViewHolder(
        private val binding: ItemConstructionLogAttachmentBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: ConstructionLogAttachment,
            editable: Boolean,
            onRemoveClick: ((ConstructionLogAttachment) -> Unit)?,
        ) {
            binding.fileNameText.text = item.fileName.ifBlank { "未命名附件" }
            binding.fileMetaText.text = listOf(
                item.mimeType.ifBlank { item.fileCategory },
                item.displaySize,
            ).joinToString("  ·  ")
            binding.removeButton.text = if (editable) "移除" else "已上传"
            binding.removeButton.isEnabled = editable
            binding.removeButton.alpha = if (editable) 1f else 0.56f
            binding.removeButton.setOnClickListener {
                if (editable) {
                    onRemoveClick?.invoke(item)
                }
            }
        }
    }
}
