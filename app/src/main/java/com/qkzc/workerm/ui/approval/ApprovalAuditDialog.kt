package com.qkzc.workerm.ui.approval

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.core.content.ContextCompat
import com.qkzc.workerm.R
import com.qkzc.workerm.data.approval.model.ApprovalItem
import com.qkzc.workerm.databinding.DialogApprovalAuditBinding

object ApprovalAuditDialog {

    fun show(
        context: Context,
        item: ApprovalItem,
        approve: Boolean,
        onConfirm: (remark: String) -> Unit,
    ) {
        val binding = DialogApprovalAuditBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.setCanceledOnTouchOutside(true)

        val actionColor = ContextCompat.getColor(
            context,
            if (approve) R.color.dashboard_blue else R.color.danger,
        )
        val actionSoftColor = ContextCompat.getColor(
            context,
            if (approve) R.color.metric_blue else R.color.metric_red,
        )

        binding.auditIcon.setImageResource(if (approve) R.drawable.ic_check_circle else R.drawable.ic_alert)
        binding.auditIcon.backgroundTintList = ColorStateList.valueOf(actionSoftColor)
        binding.auditTitle.setText(
            if (approve) {
                R.string.approval_dialog_approve_title
            } else {
                R.string.approval_dialog_reject_title
            },
        )
        binding.auditSummary.text = item.typeName
        binding.auditFormNo.text = context.getString(R.string.approval_field_form_no, item.formNo)
        binding.auditContentTitle.text = item.title.ifBlank { item.typeName }
        binding.auditRemarkInput.hint = context.getString(R.string.approval_dialog_hint)
        binding.auditRemarkInput.setText(
            if (approve) {
                R.string.approval_default_remark_approve
            } else {
                R.string.approval_default_remark_reject
            },
        )
        binding.auditRemarkInput.setSelection(binding.auditRemarkInput.text?.length ?: 0)
        binding.auditConfirmButton.backgroundTintList = ColorStateList.valueOf(actionColor)
        binding.auditCancelButton.setOnClickListener {
            dialog.dismiss()
        }
        binding.auditConfirmButton.setOnClickListener {
            onConfirm(binding.auditRemarkInput.text?.toString().orEmpty())
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.45f)
            setLayout((context.resources.displayMetrics.widthPixels * 0.9f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}
