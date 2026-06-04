package com.qkzc.workerm.ui.bracelet

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.qkzc.workerm.R
import com.qkzc.workerm.databinding.ActivityBraceletMonitorBinding

class BraceletMonitorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBraceletMonitorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBraceletMonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.braceletMonitorRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.braceletWarningEntry.setOnClickListener {
            startActivity(Intent(this, BraceletWarningActivity::class.java))
        }
        binding.braceletUnboundEntry.setOnClickListener {
            startActivity(Intent(this, BraceletUnboundActivity::class.java))
        }
        val openDetail = View.OnClickListener {
            startActivity(Intent(this, BraceletDetailActivity::class.java))
        }
        binding.root.findViewById<View>(R.id.bracelet_worker_zhang_card).setOnClickListener(openDetail)
        binding.root.findViewById<View>(R.id.bracelet_worker_li_card).setOnClickListener(openDetail)
        binding.root.findViewById<View>(R.id.bracelet_worker_wang_card).setOnClickListener(openDetail)
    }
}
