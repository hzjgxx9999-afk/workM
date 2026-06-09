package com.qkzc.workerm.ui.worker

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qkzc.workerm.R
import com.qkzc.workerm.data.worker.ManagerWorker
import com.qkzc.workerm.data.worker.entryStatusText
import com.qkzc.workerm.data.worker.memberStatusText
import com.qkzc.workerm.data.worker.normalizedBindStatus
import com.qkzc.workerm.databinding.ItemManagerWorkerBinding

class ManagerWorkerAdapter(
    private val onWorkerClick: (ManagerWorker) -> Unit,
) : ListAdapter<ManagerWorker, ManagerWorkerAdapter.WorkerViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val binding = ItemManagerWorkerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return WorkerViewHolder(binding, onWorkerClick)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WorkerViewHolder(
        private val binding: ItemManagerWorkerBinding,
        private val onWorkerClick: (ManagerWorker) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(worker: ManagerWorker) {
            val context = binding.root.context
            binding.textWorkerName.text = worker.realName.ifBlank { "未命名工人" }
            binding.textStatus.text = worker.memberStatusText()
            binding.textWorkerMeta.text = buildString {
                append("工种：")
                append(worker.workTypeName.ifBlank { "--" })
                append("    手机：")
                append(maskMobile(worker.mobile))
            }
            binding.textWorkerScope.text = buildString {
                append("班组：")
                append(worker.teamName.ifBlank { "--" })
                append("    班组长：")
                append(worker.leaderName.ifBlank { "--" })
            }
            binding.textEntryStatus.text = buildString {
                append("入场流程：")
                append(worker.entryStatusText())
                worker.enterTime.takeIf { it.isNotBlank() }?.let {
                    append("    入场：")
                    append(it)
                }
            }

            val (textColor, bgColor) = statusColors(worker)
            binding.textStatus.setTextColor(ContextCompat.getColor(context, textColor))
            binding.textStatus.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, bgColor))
            binding.root.setOnClickListener { onWorkerClick(worker) }
            binding.buttonDetail.setOnClickListener { onWorkerClick(worker) }
        }

        private fun statusColors(worker: ManagerWorker): Pair<Int, Int> {
            return when (worker.normalizedBindStatus) {
                "ACTIVE" -> R.color.success to R.color.metric_green
                "BOUND", "ENTERING" -> R.color.warning to R.color.metric_orange
                "EXITED", "LEFT" -> R.color.text_secondary to R.color.surface_muted
                "CANCELLED", "REJECTED", "ABNORMAL" -> R.color.danger to R.color.metric_red
                else -> R.color.dashboard_blue to R.color.metric_blue
            }
        }

        private fun maskMobile(mobile: String): String {
            val text = mobile.trim()
            if (text.isEmpty()) return "--"
            if (text.contains("*") || text.length < 7) return text
            return text.take(3) + "****" + text.takeLast(4)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ManagerWorker>() {
        override fun areItemsTheSame(oldItem: ManagerWorker, newItem: ManagerWorker): Boolean {
            return oldItem.workerUserId == newItem.workerUserId &&
                oldItem.projectId == newItem.projectId &&
                oldItem.relationId == newItem.relationId
        }

        override fun areContentsTheSame(oldItem: ManagerWorker, newItem: ManagerWorker): Boolean {
            return oldItem == newItem
        }
    }
}
