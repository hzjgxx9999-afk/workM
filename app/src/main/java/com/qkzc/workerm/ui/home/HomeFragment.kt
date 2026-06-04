package com.qkzc.workerm.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.MainActivity
import com.qkzc.workerm.R
import com.qkzc.workerm.data.project.ManagerProjectRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.FragmentHomeBinding
import com.qkzc.workerm.ui.bracelet.BraceletMonitorActivity
import com.qkzc.workerm.ui.material.MaterialHomeActivity
import com.qkzc.workerm.ui.project.ProjectDetailActivity
import com.qkzc.workerm.ui.video.VideoHomeActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding
        get() = checkNotNull(_binding)
    private val projectRepository = ManagerProjectRepository()
    private var currentProjectId: Long? = null

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
        binding.searchButton.setOnClickListener { toast("搜索功能静态占位") }
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
        binding.allTodoBar.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_supervision)
        }
        val openDetail = View.OnClickListener {
            openCurrentProject()
        }
        binding.root.findViewById<View>(R.id.project_crane_card).setOnClickListener(openDetail)
        binding.root.findViewById<View>(R.id.project_finance_card).setOnClickListener(openDetail)
        binding.root.findViewById<View>(R.id.project_industry_card).setOnClickListener(openDetail)
        loadHomeProject()
    }

    private fun loadHomeProject() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val session = SessionStore(requireContext().applicationContext).sessionFlow.first()
                val token = session.accessToken.takeIf { it.isNotBlank() } ?: return@launch
                projectRepository.loadProjects(token).firstOrNull()
            }.onSuccess { project ->
                currentProjectId = project?.projectId
            }
        }
    }

    private fun openCurrentProject() {
        val projectId = currentProjectId
        if (projectId == null || projectId <= 0L) {
            toast("暂无可管理项目")
            return
        }
        startActivity(
            Intent(requireContext(), ProjectDetailActivity::class.java)
                .putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, projectId),
        )
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
