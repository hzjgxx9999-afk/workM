package com.qkzc.workerm.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.qkzc.workerm.R
import com.qkzc.workerm.databinding.FragmentProfileBinding
import com.qkzc.workerm.ui.approval.ApprovalDetailActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding: FragmentProfileBinding
        get() = checkNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.findViewById<View>(R.id.approval_reissue_card).setOnClickListener {
            openApproval(ApprovalDetailActivity.TYPE_REISSUE)
        }
        binding.root.findViewById<View>(R.id.approval_leave_card).setOnClickListener {
            openApproval(ApprovalDetailActivity.TYPE_LEAVE)
        }
        binding.root.findViewById<View>(R.id.approval_loan_card).setOnClickListener {
            openApproval(ApprovalDetailActivity.TYPE_LOAN)
        }
    }

    private fun openApproval(type: String) {
        startActivity(
            Intent(requireContext(), ApprovalDetailActivity::class.java)
                .putExtra(ApprovalDetailActivity.EXTRA_TYPE, type),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
