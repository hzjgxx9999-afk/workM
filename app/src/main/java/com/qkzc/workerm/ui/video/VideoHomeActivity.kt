package com.qkzc.workerm.ui.video

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.qkzc.workerm.R
import com.qkzc.workerm.databinding.ActivityVideoHomeBinding

class VideoHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVideoHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.videoHomeRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        val openLive = View.OnClickListener {
            startActivity(Intent(this, VideoLiveActivity::class.java))
        }
        binding.root.findViewById<View>(R.id.video_tower_card).setOnClickListener(openLive)
        binding.root.findViewById<View>(R.id.video_gate_card).setOnClickListener(openLive)
        binding.root.findViewById<View>(R.id.video_warehouse_card).setOnClickListener(openLive)
        binding.root.findViewById<View>(R.id.video_living_card).setOnClickListener(openLive)
        binding.videoAnalysisEntry.setOnClickListener {
            startActivity(Intent(this, VideoAnalysisActivity::class.java))
        }
    }
}
