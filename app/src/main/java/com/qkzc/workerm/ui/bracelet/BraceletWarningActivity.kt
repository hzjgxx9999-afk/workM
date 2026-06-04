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
import com.qkzc.workerm.databinding.ActivityBraceletWarningBinding

class BraceletWarningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBraceletWarningBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBraceletWarningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.braceletWarningRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        val openDetail = View.OnClickListener {
            startActivity(Intent(this, BraceletDetailActivity::class.java))
        }
        binding.root.findViewById<View>(R.id.bracelet_warning_li_card).setOnClickListener(openDetail)
        binding.root.findViewById<View>(R.id.bracelet_warning_zhao_card).setOnClickListener(openDetail)
        binding.root.findViewById<View>(R.id.bracelet_worker_wang_card).setOnClickListener(openDetail)
    }
}
