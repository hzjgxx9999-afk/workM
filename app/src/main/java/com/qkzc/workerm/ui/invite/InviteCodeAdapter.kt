package com.qkzc.workerm.ui.invite

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.qkzc.workerm.R
import com.qkzc.workerm.data.invite.ManageInviteCode
import com.qkzc.workerm.databinding.ItemInviteCodeBinding

class InviteCodeAdapter(
    private val onCopyClick: (ManageInviteCode) -> Unit,
    private val onShareClick: (ManageInviteCode) -> Unit,
    private val onStatusClick: (ManageInviteCode) -> Unit,
) : RecyclerView.Adapter<InviteCodeAdapter.InviteCodeViewHolder>() {

    private val items = mutableListOf<ManageInviteCode>()

    fun submitList(newItems: List<ManageInviteCode>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteCodeViewHolder {
        val binding = ItemInviteCodeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return InviteCodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InviteCodeViewHolder, position: Int) {
        holder.bind(items[position], onCopyClick, onShareClick, onStatusClick)
    }

    override fun getItemCount(): Int = items.size

    class InviteCodeViewHolder(
        private val binding: ItemInviteCodeBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: ManageInviteCode,
            onCopyClick: (ManageInviteCode) -> Unit,
            onShareClick: (ManageInviteCode) -> Unit,
            onStatusClick: (ManageInviteCode) -> Unit,
        ) {
            val context = binding.root.context
            binding.textInviteCode.text = item.inviteCode.ifBlank { "--" }
            binding.textStatus.text = item.statusText
            binding.textProject.text = "项目：${item.projectName.ifBlank { item.projectId.toString() }}"
            binding.textLeader.text = "班组长：${item.leaderName.ifBlank { item.leaderId.toString() }}  班组：${item.teamName.ifBlank { "--" }}"
            binding.textUsage.text = "次数：${item.usageText}  过期：${item.expireTime.ifBlank { "未设置" }}"
            binding.textQrContent.text = "扫码内容：${item.qrContent.ifBlank { "--" }}"
            binding.buttonStatus.text = if (item.enabled) "停用" else "启用"

            val statusColor = if (item.enabled) R.color.success else R.color.text_secondary
            val badgeBg = if (item.enabled) R.color.metric_green else R.color.surface_muted
            binding.textStatus.setTextColor(ContextCompat.getColor(context, statusColor))
            binding.textStatus.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, badgeBg))

            binding.buttonCopy.setOnClickListener { onCopyClick(item) }
            binding.buttonShare.setOnClickListener { onShareClick(item) }
            binding.buttonStatus.setOnClickListener { onStatusClick(item) }
        }
    }
}
