package com.qkzc.workerm.ui.common.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.qkzc.workerm.R
import com.qkzc.workerm.data.approval.model.ApprovalItem
import com.qkzc.workerm.data.approval.model.ApprovalStatus
import com.qkzc.workerm.databinding.ItemApprovalCardBinding

class ApprovalAdapter(
    private val onApproveClick: (ApprovalItem) -> Unit,
    private val onRejectClick: (ApprovalItem) -> Unit,
) : RecyclerView.Adapter<ApprovalAdapter.ApprovalViewHolder>() {

    private val items = mutableListOf<ApprovalItem>()

    fun submitList(newItems: List<ApprovalItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApprovalViewHolder {
        val binding = ItemApprovalCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ApprovalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApprovalViewHolder, position: Int) {
        holder.bind(items[position], onApproveClick, onRejectClick)
    }

    override fun getItemCount(): Int = items.size

    class ApprovalViewHolder(
        private val binding: ItemApprovalCardBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: ApprovalItem,
            onApproveClick: (ApprovalItem) -> Unit,
            onRejectClick: (ApprovalItem) -> Unit,
        ) {
            val context = binding.root.context
            binding.typeBadgeText.text = item.typeName
            binding.titleText.text = item.title
            binding.formNoText.text = context.getString(R.string.approval_field_form_no, item.formNo)
            binding.applicantText.text = context.getString(
                R.string.approval_field_applicant,
                item.applicantName,
            )
            binding.projectText.text = context.getString(
                R.string.approval_field_project,
                item.projectName,
            )
            binding.submittedText.text = context.getString(
                R.string.approval_field_submitted_at,
                item.submittedAt,
            )
            binding.nodeText.text = context.getString(R.string.approval_field_node, item.currentNodeName)
            binding.reasonText.text = item.reason

            val isPending = item.status == ApprovalStatus.PENDING
            binding.actionContainer.isVisible = isPending
            binding.reviewGroup.isVisible = !isPending
            binding.statusText.text = item.statusName?.takeIf { it.isNotBlank() } ?: when (item.status) {
                ApprovalStatus.PENDING -> context.getString(R.string.approval_tab_pending)
                ApprovalStatus.APPROVED -> context.getString(R.string.approval_action_approve)
                ApprovalStatus.REJECTED -> context.getString(R.string.approval_action_reject)
            }

            val (textColorRes, bgColorRes) = when (item.status) {
                ApprovalStatus.PENDING -> R.color.warning to R.color.metric_orange
                ApprovalStatus.APPROVED -> R.color.success to R.color.metric_green
                ApprovalStatus.REJECTED -> R.color.danger to R.color.metric_red
            }
            binding.statusText.setTextColor(ContextCompat.getColor(context, textColorRes))
            binding.statusText.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, bgColorRes))

            if (!isPending) {
                binding.reviewLabelText.text = context.getString(R.string.approval_result_label)
                binding.reviewValueText.text = buildString {
                    if (!item.reviewResultName.isNullOrBlank()) {
                        append(item.reviewResultName)
                    }
                    if (!item.reviewRemark.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(item.reviewRemark)
                    }
                    if (!item.reviewerName.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(context.getString(R.string.approval_field_reviewer, item.reviewerName))
                    }
                    if (!item.reviewedAt.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(context.getString(R.string.approval_field_reviewed_at, item.reviewedAt))
                    }
                }
            }

            binding.approveButton.setOnClickListener {
                onApproveClick(item)
            }
            binding.rejectButton.setOnClickListener {
                onRejectClick(item)
            }
        }
    }
}
