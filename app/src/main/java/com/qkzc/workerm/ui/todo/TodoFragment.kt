package com.qkzc.workerm.ui.todo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.qkzc.workerm.MainActivity
import com.qkzc.workerm.R
import com.qkzc.workerm.databinding.FragmentTodoBinding
import com.qkzc.workerm.ui.worker.TeamLeaderActivity
import com.qkzc.workerm.ui.worker.WorkerDetailActivity
import com.qkzc.workerm.ui.worker.WorkerScanActivity

class TodoFragment : Fragment() {

    private var _binding: FragmentTodoBinding? = null
    private val binding: FragmentTodoBinding
        get() = checkNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTodoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.workerBackButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_home)
        }
        binding.scanWorkerButton.setOnClickListener {
            startActivity(Intent(requireContext(), WorkerScanActivity::class.java))
        }
        binding.teamLeaderTab.setOnClickListener {
            startActivity(Intent(requireContext(), TeamLeaderActivity::class.java))
        }
        val openWorker = View.OnClickListener {
            startActivity(Intent(requireContext(), WorkerDetailActivity::class.java))
        }
        binding.root.findViewById<View>(R.id.worker_zhang_card).setOnClickListener(openWorker)
        binding.root.findViewById<View>(R.id.worker_li_card).setOnClickListener(openWorker)
        binding.root.findViewById<View>(R.id.worker_wang_card).setOnClickListener(openWorker)
        binding.root.findViewById<View>(R.id.worker_zhao_card).setOnClickListener(openWorker)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
