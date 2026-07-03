package com.qkzc.workerm.ui.worker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.data.worker.ManagerWorkerRepository
import com.qkzc.workerm.data.worker.WorkerQrTicketParser
import com.qkzc.workerm.databinding.ActivityWorkerScanBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class WorkerScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerScanBinding
    private val workerRepository = ManagerWorkerRepository()
    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }
    private val scanner by lazy {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
    }

    private var handled = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startCamera()
        } else {
            showScanError("未授予相机权限")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWorkerScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.scanRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        binding.scanTimeText.setOnClickListener { resumeScanning() }
        showWaitingState()
        ensureCameraPermission()
    }

    private fun ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            setAnalyzer()

            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun setAnalyzer() {
        handled = false
        imageAnalysis?.setAnalyzer(cameraExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage == null || handled) {
                imageProxy.close()
                return@setAnalyzer
            }
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstOrNull()?.rawValue
                    if (!raw.isNullOrBlank()) {
                        handled = true
                        imageAnalysis?.clearAnalyzer()
                        runOnUiThread { onQrScanned(raw) }
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        }
    }

    private fun onQrScanned(content: String) {
        val ticket = WorkerQrTicketParser.parseTicket(content)
        if (ticket.isNullOrBlank()) {
            showScanError("二维码无效")
            resumeScanning()
            return
        }
        verifyTicket(ticket)
    }

    private fun verifyTicket(ticket: String) {
        lifecycleScope.launch {
            binding.scanStatusText.text = "正在核验"
            binding.scanStatusMessageText.text = "正在读取工人信息"
            runCatching {
                val session = SessionStore(this@WorkerScanActivity).sessionFlow.first()
                val projectId = intent.getLongExtra(EXTRA_PROJECT_ID, 0L)
                    .takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                workerRepository.verifyQrTicket(session.accessToken, projectId, ticket)
            }.onSuccess { result ->
                bindUiState(WorkerQrVerifyUiMapper.fromResult(result, nowText()))
            }.onFailure { throwable ->
                showScanError(throwable.message ?: "工人信息加载失败")
            }
        }
    }

    private fun resumeScanning() {
        showWaitingState()
        setAnalyzer()
    }

    private fun showWaitingState() {
        bindUiState(WorkerQrVerifyUiMapper.waiting())
    }

    private fun showScanError(message: String) {
        bindUiState(WorkerQrVerifyUiMapper.error(message))
    }

    private fun bindUiState(state: WorkerQrVerifyUiState) {
        applyTone(state.tone)
        binding.scanStatusText.text = state.statusTitle
        binding.scanStatusMessageText.text = state.statusSubtitle
        binding.workerNameText.text = state.workerName
        binding.workerTypeText.text = state.workType
        binding.workerLeaderText.text = state.teamLine
        binding.workerStatusBadgeText.text = state.presenceText
        binding.failureReasonPanel.visibility = if (state.showFailureReason) View.VISIBLE else View.GONE
        binding.failureReasonText.text = state.failureReason
        binding.projectValueText.text = "项目：${state.projectName}"
        binding.leaderValueText.text = "班组长：${state.leaderName}"
        binding.phoneValueText.text = "手机号：${state.mobile}"
        binding.idCardValueText.text = "身份证：${state.idCardNo}"
        binding.entryStatusValueText.text = state.entryValue
        binding.entryStatusHintText.text = state.entryHint
        binding.trainingStatusValueText.text = state.trainingValue
        binding.trainingStatusHintText.text = state.trainingHint
        binding.healthStatusValueText.text = state.healthValue
        binding.healthStatusHintText.text = state.healthHint
        binding.contractStatusValueText.text = state.contractValue
        binding.contractStatusHintText.text = state.contractHint
        binding.scanTimeText.text = state.footerText
    }

    private fun applyTone(tone: WorkerQrVerifyUiTone) {
        val (statusColor, iconText, iconColor, badgeBg, badgeText) = when (tone) {
            WorkerQrVerifyUiTone.PASS -> ToneColors("#16C784", "✓", "#16C784", "#DCFCE7", "#15803D")
            WorkerQrVerifyUiTone.FAIL -> ToneColors("#EF4444", "!", "#EF4444", "#FEE2E2", "#B91C1C")
            WorkerQrVerifyUiTone.EXPIRED -> ToneColors("#F59E0B", "!", "#F59E0B", "#FEF3C7", "#B45309")
            WorkerQrVerifyUiTone.WAITING -> ToneColors("#2563EB", "⌖", "#2563EB", "#DBEAFE", "#1D4ED8")
        }
        setRoundRect(binding.scanStatusContainer, statusColor, 18)
        setOval(binding.scanStatusIconText, "#FFFFFF")
        binding.scanStatusIconText.text = iconText
        binding.scanStatusIconText.setTextColor(Color.parseColor(iconColor))
        setRoundRect(binding.workerStatusBadgeText, badgeBg, 13)
        binding.workerStatusBadgeText.setTextColor(Color.parseColor(badgeText))

        val primary = Color.parseColor("#1C2733")
        val pass = Color.parseColor("#15803D")
        val fail = Color.parseColor("#B91C1C")
        binding.entryStatusValueText.setTextColor(if (tone == WorkerQrVerifyUiTone.FAIL) fail else if (tone == WorkerQrVerifyUiTone.PASS) pass else primary)
        binding.trainingStatusValueText.setTextColor(if (tone == WorkerQrVerifyUiTone.PASS) pass else primary)
        binding.healthStatusValueText.setTextColor(if (tone == WorkerQrVerifyUiTone.PASS) pass else primary)
        binding.contractStatusValueText.setTextColor(if (tone == WorkerQrVerifyUiTone.PASS) pass else primary)
    }

    private fun setRoundRect(view: View, color: String, radiusDp: Int) {
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(color))
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun setOval(view: View, color: String) {
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(color))
        }
    }

    private fun nowText(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date())
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        imageAnalysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        scanner.close()
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PROJECT_ID = "projectId"
        const val EXTRA_WORKER_ID = "workerId"
    }

    private data class ToneColors(
        val statusColor: String,
        val iconText: String,
        val iconColor: String,
        val badgeBg: String,
        val badgeText: String,
    )
}
