package com.qkzc.workerm.ui.project

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.qkzc.workerm.R
import com.qkzc.workerm.data.project.ManagerProjectTeam
import com.qkzc.workerm.databinding.ItemProjectTeamBinding

class ProjectTeamAdapter(
    private val onEditClick: (ManagerProjectTeam) -> Unit,
    private val onStatusClick: (ManagerProjectTeam) -> Unit,
) : RecyclerView.Adapter<ProjectTeamAdapter.TeamViewHolder>() {

    private val items = mutableListOf<ManagerProjectTeam>()

    fun submitList(newItems: List<ManagerProjectTeam>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val binding = ItemProjectTeamBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return TeamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        holder.bind(items[position], onEditClick, onStatusClick)
    }

    override fun getItemCount(): Int = items.size

    class TeamViewHolder(
        private val binding: ItemProjectTeamBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: ManagerProjectTeam,
            onEditClick: (ManagerProjectTeam) -> Unit,
            onStatusClick: (ManagerProjectTeam) -> Unit,
        ) {
            val context = binding.root.context
            binding.textTeamName.text = item.teamName.ifBlank { "未命名班组" }
            binding.textLeader.text = "班组长：${item.leaderName.ifBlank { item.leaderId.toString() }}"
            binding.textWorkType.text = "工种：${item.workTypeName.ifBlank { item.workTypeId?.toString() ?: "--" }}"
            binding.textStatus.text = item.statusText
            binding.buttonStatus.text = if (item.enabled) "停用" else "启用"
            binding.textRemark.text = item.remark.ifBlank { "暂无备注" }

            val statusColor = if (item.enabled) R.color.success else R.color.text_secondary
            val badgeBg = if (item.enabled) R.color.metric_green else R.color.surface_muted
            binding.textStatus.setTextColor(ContextCompat.getColor(context, statusColor))
            binding.textStatus.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, badgeBg))

            binding.buttonEdit.setOnClickListener { onEditClick(item) }
            binding.buttonStatus.setOnClickListener { onStatusClick(item) }
        }
    }
}
