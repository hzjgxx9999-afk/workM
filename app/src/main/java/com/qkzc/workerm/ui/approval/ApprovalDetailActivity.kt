package com.qkzc.workerm.ui.approval

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.R
import com.qkzc.workerm.data.approval.ApprovalRepository
import com.qkzc.workerm.data.approval.model.ApprovalCategory
import com.qkzc.workerm.data.approval.model.ApprovalItem
import com.qkzc.workerm.data.approval.model.ApprovalStatus
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityApprovalDetailBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ApprovalDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApprovalDetailBinding
    private val repository = ApprovalRepository()
    private lateinit var sessionStore: SessionStore
    private var currentItem: ApprovalItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityApprovalDetailBinding.inflate(layoutInflater)
        sessionStore = SessionStore(applicationContext)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.approvalDetailRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        binding.approveButton.setOnClickListener {
            currentItem?.let { showAuditDialog(it, approve = true) }
        }
        binding.rejectButton.setOnClickListener {
            currentItem?.let { showAuditDialog(it, approve = false) }
        }
        loadDetail()
    }

    private fun loadDetail() {
        val id = intent.getLongExtra(EXTRA_ID, 0L)
        val category = runCatching {
            ApprovalCategory.valueOf(intent.getStringExtra(EXTRA_CATEGORY).orEmpty())
        }.getOrNull()
        if (id <= 0L || category == null) {
            Toast.makeText(this, "审批参数错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        lifecycleScope.launch {
            runCatching {
                val session = sessionStore.sessionFlow.first()
                repository.detail(session.accessToken, category, id)
            }.onSuccess { item ->
                currentItem = item
                render(item)
            }.onFailure { throwable ->
                Toast.makeText(
                    this@ApprovalDetailActivity,
                    throwable.message ?: "审批详情加载失败",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun render(item: ApprovalItem) {
        val pending = item.status == ApprovalStatus.PENDING
        binding.titleText.text = "${item.typeName}详情"
        binding.tipText.isVisible = pending
        binding.attachmentCard.isVisible = item.category == ApprovalCategory.EXCEPTION
        binding.approveButton.isVisible = pending
        binding.rejectButton.isVisible = pending
        binding.applicantIcon.text = item.typeName.take(1)
        binding.applicantName.text = item.applicantName.ifBlank { "未知申请人" }
        binding.applicantPhone.text = if (item.applicantMobile.isBlank()) {
            "申请单：${item.formNo}"
        } else {
            "手机号：${item.applicantMobile}"
        }
        binding.applicantNo.text = "提交时间：${item.submittedAt.ifBlank { "-" }}"
        binding.mainInfoTitle.text = item.typeName
        binding.mainInfoText.text = item.title.ifBlank { item.typeName }
        binding.reasonTitle.text = "申请说明"
        binding.reasonText.text = item.reason.ifBlank { "无" }
        binding.projectNameText.text = item.projectName.ifBlank { "未知项目" }
        binding.projectAddressText.text = item.projectAddress.ifBlank { "项目地址：-" }
        binding.contractorUnitText.text = "总包单位：${item.contractorUnit.ifBlank { "-" }}"
        binding.projectStatusText.text = item.projectStatusName.ifBlank { "施工中" }
        binding.teamText.text = buildString {
            append(item.teamName.ifBlank { "未分配班组" })
            append("    负责人：")
            append(item.leaderName.ifBlank { "-" })
        }
        binding.statusText.text = item.statusName ?: item.status.toDisplayName()
        binding.resultStatusText.text = item.reviewResultName ?: item.statusName ?: item.status.toDisplayName()
    }

    private fun showAuditDialog(item: ApprovalItem, approve: Boolean) {
        val input = EditText(this).apply {
            hint = getString(R.string.approval_dialog_hint)
            minLines = 3
            setSingleLine(false)
            setText(
                if (approve) {
                    R.string.approval_default_remark_approve
                } else {
                    R.string.approval_default_remark_reject
                },
            )
        }
        AlertDialog.Builder(this)
            .setTitle(
                if (approve) {
                    R.string.approval_dialog_approve_title
                } else {
                    R.string.approval_dialog_reject_title
                },
            )
            .setView(input)
            .setNegativeButton(R.string.approval_action_cancel, null)
            .setPositiveButton(R.string.approval_action_confirm) { _, _ ->
                audit(item, approve, input.text?.toString().orEmpty())
            }
            .show()
    }

    private fun audit(item: ApprovalItem, approve: Boolean, remark: String) {
        lifecycleScope.launch {
            runCatching {
                val session = sessionStore.sessionFlow.first()
                repository.audit(session.accessToken, item, approve, remark)
            }.onSuccess {
                Toast.makeText(
                    this@ApprovalDetailActivity,
                    if (approve) R.string.approval_toast_approved else R.string.approval_toast_rejected,
                    Toast.LENGTH_SHORT,
                ).show()
                loadDetail()
            }.onFailure { throwable ->
                Toast.makeText(
                    this@ApprovalDetailActivity,
                    throwable.message ?: "审批操作失败",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun ApprovalStatus.toDisplayName(): String {
        return when (this) {
            ApprovalStatus.PENDING -> "待审批"
            ApprovalStatus.APPROVED -> "同意"
            ApprovalStatus.REJECTED -> "驳回"
        }
    }

    companion object {
        const val EXTRA_CATEGORY = "approval_category"
        const val EXTRA_ID = "approval_id"
    }
}
