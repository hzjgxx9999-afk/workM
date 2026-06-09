package com.qkzc.workerm.ui.todo

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.R
import com.qkzc.workerm.data.approval.ApprovalRepository
import com.qkzc.workerm.data.approval.model.ApprovalFilter
import com.qkzc.workerm.data.approval.model.ApprovalItem
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.FragmentApprovalCenterBinding
import com.qkzc.workerm.ui.approval.ApprovalDetailActivity
import com.qkzc.workerm.ui.common.adapter.ApprovalAdapter
import kotlinx.coroutines.launch

class TodoFragment : Fragment() {

    private var _binding: FragmentApprovalCenterBinding? = null
    private val binding: FragmentApprovalCenterBinding
        get() = checkNotNull(_binding)

    private lateinit var viewModel: TodoViewModel
    private val adapter = ApprovalAdapter(
        onApproveClick = { showAuditDialog(it, approve = true) },
        onRejectClick = { showAuditDialog(it, approve = false) },
        onItemClick = { openDetail(it) },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentApprovalCenterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            TodoViewModel.Factory(
                approvalRepository = ApprovalRepository(),
                sessionStore = SessionStore(requireContext().applicationContext),
            ),
        )[TodoViewModel::class.java]

        binding.approvalRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.approvalRecycler.adapter = adapter
        binding.pendingTab.setOnClickListener { viewModel.load(ApprovalFilter.PENDING) }
        binding.processedTab.setOnClickListener { viewModel.load(ApprovalFilter.PROCESSED) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                render(state)
            }
        }
        viewModel.load()
    }

    private fun render(state: ApprovalUiState) {
        binding.approvalLoading.isVisible = state.loading
        binding.pendingCountText.text = "待审批\n${state.summary.pendingCount} 件"
        binding.processedCountText.text = "已处理\n${state.summary.processedCount} 件"
        binding.rejectedCountText.text = "已驳回\n${state.summary.rejectedCount} 件"
        adapter.submitList(state.approvals)
        binding.approvalRecycler.isVisible = state.approvals.isNotEmpty()
        binding.approvalEmptyText.isVisible = state.approvals.isEmpty() && !state.loading
        renderTabs(state.filter)
        state.errorMessage?.let {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            viewModel.consumeError()
        }
    }

    private fun renderTabs(filter: ApprovalFilter) {
        val selected = ContextCompat.getColor(requireContext(), R.color.dashboard_blue)
        val normal = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        binding.pendingTab.setTextColor(if (filter == ApprovalFilter.PENDING) selected else normal)
        binding.processedTab.setTextColor(if (filter == ApprovalFilter.PROCESSED) selected else normal)
    }

    private fun showAuditDialog(item: ApprovalItem, approve: Boolean) {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.approval_dialog_hint)
            setSingleLine(false)
            minLines = 3
            setText(
                if (approve) {
                    R.string.approval_default_remark_approve
                } else {
                    R.string.approval_default_remark_reject
                },
            )
        }
        AlertDialog.Builder(requireContext())
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
                viewModel.audit(item, approve, input.text?.toString().orEmpty())
                Toast.makeText(
                    requireContext(),
                    if (approve) R.string.approval_toast_approved else R.string.approval_toast_rejected,
                    Toast.LENGTH_SHORT,
                ).show()
            }
            .show()
    }

    private fun openDetail(item: ApprovalItem) {
        startActivity(
            Intent(requireContext(), ApprovalDetailActivity::class.java)
                .putExtra(ApprovalDetailActivity.EXTRA_CATEGORY, item.category.name)
                .putExtra(ApprovalDetailActivity.EXTRA_ID, item.id),
        )
    }

    override fun onDestroyView() {
        binding.approvalRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
