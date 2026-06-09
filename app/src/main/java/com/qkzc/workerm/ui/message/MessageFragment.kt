package com.qkzc.workerm.ui.message

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

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
            .setView(buildDetailView(detail))
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

    private fun buildDetailView(detail: AiWarningDetail): View {
        val context = requireContext()
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4.dp(), 6.dp(), 4.dp(), 2.dp())
        }

        val photoUrl = detail.item.photoUrl?.trim().orEmpty()
        if (photoUrl.isNotBlank()) {
            val image = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    168.dp(),
                ).apply {
                    bottomMargin = 12.dp()
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.parseColor("#E8EDF5"))
            }
            content.addView(image)
            loadWarningPhoto(photoUrl, image)
        }

        content.addView(sectionCard {
            addView(sectionTitle("巡查结论"))
            addView(bodyText(detail.summaryOrContent().ifBlank { "暂无AI结论" }))
            addView(bodyText("状态：${detail.item.handleStatusText}    风险：${detail.item.riskLevelText}    分值：${detail.item.riskScore ?: "-"}"))
        })

        content.addView(sectionCard {
            addView(sectionTitle("现场信息"))
            addView(bodyText(detailLine("项目", detail.item.projectName)))
            addView(bodyText(detailLine("工人", detail.item.workerName)))
            addView(bodyText(detailLine("区域", detail.item.sceneName)))
            addView(bodyText(detailLine("点位", detail.item.inspectionPoint)))
            addView(bodyText(detailLine("工种", detail.item.workType)))
            addView(bodyText(detailLine("拍摄", detail.captureTime.ifBlank { detail.item.createTime })))
        })

        if (detail.needManualReview || detail.manualReviewReason.isNotBlank()) {
            content.addView(sectionCard {
                addView(sectionTitle("人工复核"))
                addView(bodyText(if (detail.needManualReview) "需要人工复核" else "不需要人工复核"))
                if (detail.manualReviewReason.isNotBlank()) {
                    addView(bodyText(detail.manualReviewReason))
                }
            })
        }

        content.addView(sectionCard {
            addView(sectionTitle("隐患明细"))
            if (detail.hazards.isEmpty()) {
                addView(bodyText("暂无隐患明细"))
            } else {
                detail.hazards.forEachIndexed { index, hazard ->
                    addView(bodyText("${index + 1}. ${hazard.name.ifBlank { hazard.code.ifBlank { "未命名隐患" } }} · ${hazard.level.ifBlank { "未分级" }}"))
                    hazard.confidence?.let { addView(bodyText("置信度：${"%.0f".format(it * 100)}%")) }
                    if (hazard.evidence.isNotBlank()) addView(bodyText("证据：${hazard.evidence}"))
                    if (hazard.advice.isNotBlank()) addView(bodyText("建议：${hazard.advice}"))
                    if (index != detail.hazards.lastIndex) addView(space(8))
                }
            }
        })

        if (!detail.errorMsg.isNullOrBlank()) {
            content.addView(sectionCard {
                addView(sectionTitle("分析异常"))
                addView(bodyText(detail.errorMsg))
            })
        }

        return ScrollView(context).apply {
            addView(content)
        }
    }

    private fun loadWarningPhoto(photoUrl: String, target: ImageView) {
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    URL(photoUrl).openStream().use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }.getOrNull()
            }
            if (bitmap != null && isAdded) {
                target.setImageBitmap(bitmap)
            } else {
                target.setImageDrawable(null)
            }
        }
    }

    private fun sectionCard(build: LinearLayout.() -> Unit): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 12.dp().toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = 10.dp()
            }
            build()
        }
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 6.dp())
        }
    }

    private fun bodyText(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            textSize = 13f
            setLineSpacing(2.dp().toFloat(), 1.0f)
            setPadding(0, 2.dp(), 0, 2.dp())
        }
    }

    private fun space(height: Int): View {
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height.dp(),
            )
        }
    }

    private fun AiWarningDetail.summaryOrContent(): String {
        return item.summary.ifBlank { item.content }
    }

    private fun detailLine(label: String, value: String): String {
        return "$label：${value.ifBlank { "-" }}"
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        binding.warningRecycler.adapter = null
        _binding = null
    }
}
