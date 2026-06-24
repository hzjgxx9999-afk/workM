package com.qkzc.workerm.ui.dispatch

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.data.dispatch.DispatchRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityDispatchListBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DispatchListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDispatchListBinding
    private val repository = DispatchRepository()
    private val adapter = ManagerDispatchAdapter {
        startActivity(DispatchDetailActivity.intent(this, it.id))
    }
    private val statuses: List<String?> = listOf(
        null,
        "PENDING_DELEGATION",
        "IN_PROGRESS",
        "PENDING_MANAGER_ACCEPTANCE",
        "COMPLETED",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDispatchListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backButton.setOnClickListener { finish() }
        binding.createButton.setOnClickListener { startActivity(Intent(this, DispatchCreateActivity::class.java)) }
        binding.dispatchRecycler.layoutManager = LinearLayoutManager(this)
        binding.dispatchRecycler.adapter = adapter
        binding.statusSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("全部", "待分派", "处理中", "待验收", "已完成"),
        )
        binding.statusSpinner.setSelection(0, false)
        binding.statusSpinner.onItemSelectedListener = SimpleItemSelectedListener { load(statuses[it]) }
        load(null)
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) load(statuses[binding.statusSpinner.selectedItemPosition])
    }

    private fun load(status: String?): Unit {
        lifecycleScope.launch {
            runCatching {
                val token = SessionStore(applicationContext).sessionFlow.first().accessToken
                repository.page(token, status)
            }.onSuccess {
                adapter.submitList(it)
                binding.emptyText.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure {
                adapter.submitList(emptyList())
                binding.emptyText.text = "加载失败，点击重试"
                binding.emptyText.visibility = View.VISIBLE
                binding.emptyText.setOnClickListener { load(status) }
            }
        }
    }
}
