package com.qkzc.workerm.ui.material

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.qkzc.workerm.R
import com.qkzc.workerm.databinding.ActivityMaterialListBinding

class MaterialListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaterialListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMaterialListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.materialListRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        binding.root.findViewById<View>(R.id.material_steel_card).setOnClickListener {
            startActivity(Intent(this, MaterialDetailActivity::class.java))
        }
        binding.root.findViewById<View>(R.id.material_cement_card).setOnClickListener {
            startActivity(Intent(this, MaterialDetailActivity::class.java))
        }
    }
}
