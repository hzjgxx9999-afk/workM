package com.qkzc.workerm.ui.dispatch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.qkzc.workerm.databinding.ActivityDispatchPhotoPreviewBinding

class DispatchPhotoPreviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDispatchPhotoPreviewBinding
    private var urls = emptyList<String>()
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDispatchPhotoPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        urls = intent.getStringArrayListExtra(EXTRA_URLS).orEmpty()
        index = intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(0, (urls.size - 1).coerceAtLeast(0))
        binding.closeButton.setOnClickListener { finish() }
        binding.photoImage.setOnClickListener {
            if (urls.isNotEmpty()) { index = (index + 1) % urls.size; render() }
        }
        render()
    }

    private fun render() {
        binding.photoImage.load(urls.getOrNull(index)) { crossfade(true) }
        binding.countText.text = if (urls.isEmpty()) "暂无照片" else "${index + 1}/${urls.size}"
    }

    companion object {
        private const val EXTRA_URLS = "urls"
        private const val EXTRA_INDEX = "index"
        fun intent(context: Context, urls: List<String>, index: Int = 0) =
            Intent(context, DispatchPhotoPreviewActivity::class.java)
                .putStringArrayListExtra(EXTRA_URLS, ArrayList(urls))
                .putExtra(EXTRA_INDEX, index)
    }
}
