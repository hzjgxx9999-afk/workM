package com.qkzc.workerm.ui.message

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.MainActivity
import com.qkzc.workerm.R
import com.qkzc.workerm.data.aiwarning.AiWarningRepository
import com.qkzc.workerm.data.aiwarning.model.AiWarningDetail
import com.qkzc.workerm.data.aiwarning.model.AiWarningHandleStatus
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.FragmentMessageBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MessageFragment : Fragment() {

    private var _binding: FragmentMessageBinding? = null
    private val binding: FragmentMessageBinding
        get() = checkNotNull(_binding)
    private val viewModel: MessageViewModel by viewModels {
        MessageViewModel.Factory(
            AiWarningRepository(),
            SessionStore(requireContext().applicationContext),
        )
    }
    private val adapter = AiWarningAdapter(
        onItemClick = { item -> viewModel.openDetail(item) },
        onMarkReadClick = { item -> viewModel.markRead(item) },
        onHandleClick = { item -> viewModel.handle(item) },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        setupActions()
        observeState()
        viewModel.load()
    }

    private fun setupList() {
        binding.warningRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.warningRecycler.adapter = adapter
    }

    private fun setupActions() {
        binding.backButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_home)
        }
        binding.refreshButton.setOnClickListener {
            viewModel.load()
        }
        binding.tabAll.setOnClickListener { viewModel.load(AiWarningTab.ALL) }
        binding.tabHigh.setOnClickListener { viewModel.load(AiWarningTab.HIGH) }
        binding.tabPending.setOnClickListener { viewModel.load(AiWarningTab.PENDING) }
        binding.tabHandled.setOnClickListener { viewModel.load(AiWarningTab.HANDLED) }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderState(state)
                    state.selectedDetail?.let { detail ->
                        showDetailDialog(detail)
                        viewModel.consumeDetail()
                    }
                    state.errorMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        viewModel.consumeError()
                    }
                }
            }
        }
    }

    private fun renderState(state: AiWarningUiState) {
        adapter.submitList(state.warnings)
        binding.loadingProgress.isVisible = state.loading || state.actionLoading
        binding.emptyText.isVisible = !state.loading && state.warnings.isEmpty()
        binding.warningRecycler.isVisible = state.warnings.isNotEmpty()
        binding.warningScoreText.text = state.maxRiskScore.toString()
        binding.warningScoreDeltaText.text = "当前最高风险分\n高风险 ${state.highCount} 条"
        binding.warningTotalText.text = "全部预警 ${state.totalCount}条"
        binding.warningPendingText.text = "待处理 ${state.pendingCount}条"
        binding.warningHandledText.text = "未读 ${state.unreadCount}条"
        renderTabs(state.selectedTab)
    }

    private fun renderTabs(selectedTab: AiWarningTab) {
        val tabs = listOf(
            binding.tabAll to AiWarningTab.ALL,
            binding.tabHigh to AiWarningTab.HIGH,
            binding.tabPending to AiWarningTab.PENDING,
            binding.tabHandled to AiWarningTab.HANDLED,
        )
        tabs.forEach { (view, tab) ->
            val selected = tab == selectedTab
            view.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.dashboard_blue else R.color.text_secondary,
                ),
            )
            view.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun showDetailDialog(detail: AiWarningDetail) {
        val pending = detail.item.handleStatus == AiWarningHandleStatus.PENDING
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(detail.item.title)
            .setMessage(detail.toDialogMessage())
            .setNegativeButton("关闭", null)
        if (pending) {
            builder
                .setPositiveButton("标记已处理") { _, _ ->
                    viewModel.handleDetail(detail, AiWarningHandleStatus.HANDLED)
                }
                .setNeutralButton("忽略") { _, _ ->
                    viewModel.handleDetail(detail, AiWarningHandleStatus.IGNORED)
                }
        }
        builder.show()
    }

    private fun AiWarningDetail.toDialogMessage(): String {
        val hazardsText = hazards.takeIf { it.isNotEmpty() }?.joinToString(separator = "\n\n") { hazard ->
            buildString {
                append(hazard.name.ifBlank { hazard.code.ifBlank { "未命名隐患" } })
                if (hazard.level.isNotBlank()) append("（${hazard.level}）")
                hazard.confidence?.let { append("  置信度 ${"%.0f".format(it * 100)}%") }
                if (hazard.evidence.isNotBlank()) append("\n证据：${hazard.evidence}")
                if (hazard.advice.isNotBlank()) append("\n建议：${hazard.advice}")
            }
        } ?: "暂无隐患明细"

        return buildString {
            appendLine(detailLine("项目", item.projectName))
            appendLine(detailLine("工人", item.workerName))
            appendLine(detailLine("位置", item.inspectionPoint.ifBlank { item.sceneName }))
            appendLine(detailLine("风险", "${item.riskLevelText} ${item.riskScore?.let { "$it 分" } ?: ""}".trim()))
            appendLine(detailLine("时间", captureTime.ifBlank { item.createTime }))
            if (summaryOrContent().isNotBlank()) {
                appendLine()
                appendLine(summaryOrContent())
            }
            appendLine()
            appendLine(hazardsText)
            if (needManualReview || manualReviewReason.isNotBlank()) {
                appendLine()
                append("人工复核：")
                append(if (needManualReview) "需要" else "不需要")
                if (manualReviewReason.isNotBlank()) append("，$manualReviewReason")
            }
            if (!errorMsg.isNullOrBlank()) {
                appendLine()
                append("分析异常：$errorMsg")
            }
        }
    }

    private fun AiWarningDetail.summaryOrContent(): String {
        return item.summary.ifBlank { item.content }
    }

    private fun detailLine(label: String, value: String): String {
        return "$label：${value.ifBlank { "-" }}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.warningRecycler.adapter = null
        _binding = null
    }
}
