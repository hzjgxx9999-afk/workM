package com.qkzc.workerm.ui.worker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qkzc.workerm.data.worker.ManagerTeamLeaderSummary
import com.qkzc.workerm.databinding.ItemManagerTeamLeaderBinding

class ManagerTeamLeaderAdapter(
    private val onMembersClick: (ManagerTeamLeaderSummary) -> Unit,
    private val onDispatchClick: (ManagerTeamLeaderSummary) -> Unit,
) : ListAdapter<ManagerTeamLeaderSummary, ManagerTeamLeaderAdapter.TeamLeaderViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamLeaderViewHolder {
        val binding = ItemManagerTeamLeaderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return TeamLeaderViewHolder(binding, onMembersClick, onDispatchClick)
    }

    override fun onBindViewHolder(holder: TeamLeaderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TeamLeaderViewHolder(
        private val binding: ItemManagerTeamLeaderBinding,
        private val onMembersClick: (ManagerTeamLeaderSummary) -> Unit,
        private val onDispatchClick: (ManagerTeamLeaderSummary) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ManagerTeamLeaderSummary) {
            binding.textLeaderName.text = item.leaderName.ifBlank { "未命名班组长" }
            binding.textAbnormalCount.text = "异常 ${item.abnormalCount}"
            binding.textLeaderMeta.text = buildString {
                append("班组人数：")
                append(item.workerCount)
                append("人    负责项目：")
                append(item.projectCount)
                append("个")
                item.mobile.takeIf { it.isNotBlank() }?.let {
                    append("\n联系方式：")
                    append(maskMobile(it))
                }
            }
            binding.textScope.text = item.scopes.take(2).joinToString("  ") { scope ->
                "${scope.projectName.ifBlank { "项目 ${scope.projectId}" }} · ${scope.teamName.ifBlank { "班组 ${scope.teamId}" }}"
            }.ifBlank { "暂无负责范围" }
            binding.buttonViewMembers.setOnClickListener { onMembersClick(item) }
            binding.buttonDispatch.setOnClickListener { onDispatchClick(item) }
        }

        private fun maskMobile(mobile: String): String {
            val text = mobile.trim()
            if (text.isEmpty()) return "--"
            if (text.contains("*") || text.length < 7) return text
            return text.take(3) + "****" + text.takeLast(4)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ManagerTeamLeaderSummary>() {
        override fun areItemsTheSame(
            oldItem: ManagerTeamLeaderSummary,
            newItem: ManagerTeamLeaderSummary,
        ): Boolean = oldItem.leaderId == newItem.leaderId

        override fun areContentsTheSame(
            oldItem: ManagerTeamLeaderSummary,
            newItem: ManagerTeamLeaderSummary,
        ): Boolean = oldItem == newItem
    }
}
