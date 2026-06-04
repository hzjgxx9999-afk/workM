package com.qkzc.workerm.ui.supervision

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.data.project.ManagerProject
import com.qkzc.workerm.data.project.ManagerProjectRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.FragmentSupervisionBinding
import com.qkzc.workerm.ui.project.ManagerProjectAdapter
import com.qkzc.workerm.ui.project.ProjectDetailActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SupervisionFragment : Fragment() {

    private var _binding: FragmentSupervisionBinding? = null
    private val binding: FragmentSupervisionBinding
        get() = checkNotNull(_binding)

    private val projectRepository = ManagerProjectRepository()
    private lateinit var sessionStore: SessionStore
    private val projectAdapter = ManagerProjectAdapter { project -> openProject(project) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSupervisionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionStore = SessionStore(requireContext().applicationContext)
        binding.projectRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.projectRecycler.adapter = projectAdapter
        binding.projectRefreshButton.setOnClickListener { loadProjects() }
        binding.addProjectButton.setOnClickListener {
            Toast.makeText(requireContext(), "请在后台管理端新增项目", Toast.LENGTH_SHORT).show()
        }
        loadProjects()
    }

    private fun loadProjects() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val session = sessionStore.sessionFlow.first()
                val token = session.accessToken.takeIf { it.isNotBlank() } ?: error("请先登录")
                projectRepository.loadProjects(token)
            }.onSuccess { projects ->
                projectAdapter.submitList(projects)
                binding.projectRecycler.isVisible = projects.isNotEmpty()
                binding.projectEmptyText.isVisible = projects.isEmpty()
                renderSummary(projects)
            }.onFailure { throwable ->
                binding.projectRecycler.isVisible = false
                binding.projectEmptyText.isVisible = true
                binding.projectSummaryText.text = "加载失败"
                Toast.makeText(
                    requireContext(),
                    throwable.message ?: "项目列表加载失败",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun renderSummary(projects: List<ManagerProject>) {
        val constructionCount = projects.count { it.projectStatus == "CONSTRUCTION" }
        val pausedCount = projects.count { it.projectStatus == "PAUSED" }
        val completedCount = projects.count { it.projectStatus == "COMPLETED" }
        binding.projectSummaryText.text = "施工中 $constructionCount    暂停 $pausedCount    已完成 $completedCount"
    }

    private fun openProject(project: ManagerProject) {
        startActivity(
            Intent(requireContext(), ProjectDetailActivity::class.java)
                .putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, project.projectId),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.projectRecycler.adapter = null
        _binding = null
    }
}
