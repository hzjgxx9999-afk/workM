package com.qkzc.workerm.ui.cad

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.qkzc.workerm.databinding.ActivityCadPreviewBinding

class CadPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCadPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.cadPreviewRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        binding.titleText.text = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "CAD 预览" }
        configureWebView()
        val previewUrl = intent.getStringExtra(EXTRA_PREVIEW_URL).orEmpty()
        if (previewUrl.isBlank()) {
            showError("缺少预览地址，暂时无法打开图纸")
            return
        }
        binding.cadWebView.loadUrl(previewUrl)
    }

    private fun configureWebView() {
        binding.cadWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = false
            displayZoomControls = false
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(true)
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        binding.cadWebView.webChromeClient = WebChromeClient()
        binding.cadWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.loadingBar.isVisible = true
                binding.errorText.isVisible = false
                binding.cadWebView.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.loadingBar.isVisible = false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    showError(error?.description?.toString().orEmpty().ifBlank { "预览页加载失败，请检查网络后重试" })
                }
            }
        }
    }

    override fun onBackPressed() {
        if (::binding.isInitialized && binding.cadWebView.canGoBack()) {
            binding.cadWebView.goBack()
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            binding.cadWebView.apply {
                stopLoading()
                webChromeClient = null
                destroy()
            }
        }
        super.onDestroy()
    }

    private fun showError(message: String) {
        binding.loadingBar.isVisible = false
        binding.cadWebView.visibility = View.GONE
        binding.errorText.isVisible = true
        binding.errorText.text = message
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_PREVIEW_URL = "extra_preview_url"
        const val DEFAULT_PREVIEW_URL = "https://mlightcad.github.io/cad-viewer/"
    }
}
