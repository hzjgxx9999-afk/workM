package com.qkzc.workerm.ui.material

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.qkzc.workerm.databinding.ActivityMaterialHomeBinding

class MaterialHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaterialHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMaterialHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.materialHomeRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.materialListEntry.setOnClickListener {
            startActivity(Intent(this, MaterialListActivity::class.java))
        }
        binding.materialReportEntry.setOnClickListener {
            startActivity(Intent(this, MaterialReportActivity::class.java))
        }
    }
}
