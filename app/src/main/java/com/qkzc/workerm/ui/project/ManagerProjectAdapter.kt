package com.qkzc.workerm.ui.project

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qkzc.workerm.data.project.ManagerProject
import com.qkzc.workerm.databinding.ItemManagerProjectCardBinding

class ManagerProjectAdapter(
    private val onProjectClick: (ManagerProject) -> Unit,
) : ListAdapter<ManagerProject, ManagerProjectAdapter.ProjectViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemManagerProjectCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ProjectViewHolder(binding, onProjectClick)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProjectViewHolder(
        private val binding: ItemManagerProjectCardBinding,
        private val onProjectClick: (ManagerProject) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(project: ManagerProject) {
            binding.projectNameText.text = project.projectName.ifBlank { "未命名项目" }
            binding.projectAddressText.text = project.projectAddress.ifBlank { "暂无项目地址" }
            binding.projectStatusText.text = project.projectStatus.toStatusText()
            binding.projectProgressText.text = "${project.progressPercent}%"
            binding.projectProgressBar.progress = project.progressPercent
            binding.projectManagerText.text = "经理 ${project.projectManagerName.ifBlank { "-" }}"
            binding.projectWorkerText.text = "在场 ${project.workerCount} 人"
            binding.projectFinishText.text = project.plannedFinishDate.takeIf { it.isNotBlank() } ?: "未设竣工"
            binding.root.setOnClickListener { onProjectClick(project) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ManagerProject>() {
        override fun areItemsTheSame(oldItem: ManagerProject, newItem: ManagerProject): Boolean {
            return oldItem.projectId == newItem.projectId
        }

        override fun areContentsTheSame(oldItem: ManagerProject, newItem: ManagerProject): Boolean {
            return oldItem == newItem
        }
    }
}

fun String.toStatusText(): String {
    return when (this) {
        "CONSTRUCTION" -> "施工中"
        "PAUSED" -> "暂停"
        "COMPLETED" -> "已完成"
        else -> "未知"
    }
}
