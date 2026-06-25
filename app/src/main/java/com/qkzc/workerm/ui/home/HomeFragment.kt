package com.qkzc.workerm.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.MainActivity
import com.qkzc.workerm.R
import com.qkzc.workerm.data.aiwarning.AiWarningRepository
import com.qkzc.workerm.data.dispatch.DispatchRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.FragmentHomeBinding
import com.qkzc.workerm.ui.bracelet.BraceletMonitorActivity
import com.qkzc.workerm.ui.material.MaterialHomeActivity
import com.qkzc.workerm.ui.dispatch.DispatchDetailActivity
import com.qkzc.workerm.ui.dispatch.DispatchListActivity
import com.qkzc.workerm.ui.dispatch.ManagerDispatchAdapter
import com.qkzc.workerm.ui.video.VideoHomeActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding
        get() = checkNotNull(_binding)
    private val dispatchRepository = DispatchRepository()
    private val aiWarningRepository = AiWarningRepository()
    private val dispatchAdapter = ManagerDispatchAdapter { order ->
        startActivity(DispatchDetailActivity.intent(requireContext(), order.id))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.homeDispatchRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.homeDispatchRecycler.adapter = dispatchAdapter

        binding.notificationButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_message)
        }
        binding.aiWarningCard.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_message)
        }
        binding.approvalCenterAction.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_profile)
        }
        binding.materialAction.setOnClickListener {
            startActivity(Intent(requireContext(), MaterialHomeActivity::class.java))
        }
        binding.videoAction.setOnClickListener {
            startActivity(Intent(requireContext(), VideoHomeActivity::class.java))
        }
        binding.braceletAction.setOnClickListener {
            startActivity(Intent(requireContext(), BraceletMonitorActivity::class.java))
        }
//        binding.inviteCodeAction.setOnClickListener {
//            (activity as? MainActivity)?.openInviteCodeManage()
//        }
        binding.allTodoBar.setOnClickListener {
            startActivity(Intent(requireContext(), DispatchListActivity::class.java))
        }
        renderAiWarningCard(HomeAiWarningCardState.loading())
        loadDispatches()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            loadAiWarningSummary()
        }
    }

    private fun loadAiWarningSummary() {
        viewLifecycleOwner.lifecycleScope.launch {
            renderAiWarningCard(HomeAiWarningCardState.loading())
            runCatching {
                val session = SessionStore(requireContext().applicationContext).sessionFlow.first()
                val token = session.accessToken.takeIf { it.isNotBlank() } ?: error("请先登录")
                aiWarningRepository.loadSummary(token)
            }.onSuccess { summary ->
                renderAiWarningCard(HomeAiWarningCardState.fromSummary(summary))
            }.onFailure {
                renderAiWarningCard(HomeAiWarningCardState.error())
            }
        }
    }

    private fun renderAiWarningCard(state: HomeAiWarningCardState) {
        binding.homeAiWarningScoreText.text = state.scoreText
        binding.homeAiWarningMetricTitleText.text = state.metricTitle
        binding.homeAiWarningMetricSubtitleText.text = state.metricSubtitle
        binding.homeAiWarningTotalChip.text = state.chips.getOrNull(0).orEmpty()
        binding.homeAiWarningPendingChip.text = state.chips.getOrNull(1).orEmpty()
        binding.homeAiWarningUnreadChip.text = state.chips.getOrNull(2).orEmpty()
    }

    private fun loadDispatches() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val session = SessionStore(requireContext().applicationContext).sessionFlow.first()
                val token = session.accessToken.takeIf { it.isNotBlank() } ?: return@launch
                dispatchRepository.recent(token)
            }.onSuccess { orders ->
                dispatchAdapter.submitList(orders)
                binding.homeDispatchRecycler.isVisible = orders.isNotEmpty()
                binding.homeDispatchEmptyText.isVisible = orders.isEmpty()
            }.onFailure {
                dispatchAdapter.submitList(emptyList())
                binding.homeDispatchRecycler.isVisible = false
                binding.homeDispatchEmptyText.isVisible = true
                binding.homeDispatchEmptyText.text = "派工动态加载失败，点击重试"
                binding.homeDispatchEmptyText.setOnClickListener { loadDispatches() }
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.homeDispatchRecycler.adapter = null
        _binding = null
    }
}
