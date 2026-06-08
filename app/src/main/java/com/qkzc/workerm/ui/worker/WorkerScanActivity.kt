package com.qkzc.workerm.ui.worker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.qkzc.workerm.data.worker.ManagerWorker
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
            runCatching {
                val session = SessionStore(this@WorkerScanActivity).sessionFlow.first()
                val projectId = intent.getLongExtra(EXTRA_PROJECT_ID, 0L)
                    .takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                workerRepository.scanQrTicket(session.accessToken, projectId, ticket)
            }.onSuccess { worker ->
                bindWorker(worker)
                binding.scanStatusText.text = "扫描成功"
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
        binding.scanStatusText.text = "对准二维码"
        binding.workerNameText.text = "等待扫码"
        binding.workerTypeText.text = "请扫描工人端个人二维码"
        binding.workerLeaderText.text = "管理端会按当前项目范围核验"
        binding.workerDetailText.text = ""
        binding.scanTimeText.text = "识别成功后自动核验"
    }

    private fun showScanError(message: String) {
        binding.scanStatusText.text = "核验失败"
        binding.workerNameText.text = "暂无工人"
        binding.workerTypeText.text = message
        binding.workerLeaderText.text = "请确认二维码和项目范围"
        binding.workerDetailText.text = ""
        binding.scanTimeText.text = "点击继续扫码 >"
    }

    private fun bindWorker(worker: ManagerWorker) {
        val teamName = worker.teamName.ifBlank { worker.leaderName.ifBlank { "未绑定班组" } }
        binding.workerNameText.text = worker.realName.ifBlank { "未命名工人" }
        binding.workerTypeText.text = worker.workTypeName.ifBlank { "未配置工种" }
        binding.workerLeaderText.text = "所属班组：$teamName"
        binding.workerDetailText.text = buildString {
            append("绑定状态：")
            append(worker.bindStatus.ifBlank { "暂无" })
            append('\n')
            append("入场状态：")
            append(worker.entryStatus.ifBlank { "暂无" })
            append('\n')
            append("身份证号：")
            append(maskIdCard(worker.idCardNo))
            append('\n')
            append("所属项目：")
            append(worker.projectName.ifBlank { "暂无" })
        }
        binding.scanTimeText.text = "扫描时间：${nowText()}        点击继续扫码 >"
    }

    private fun maskIdCard(idCardNo: String): String {
        if (idCardNo.length < 8) return "暂无"
        return idCardNo.take(3) + "************" + idCardNo.takeLast(4)
    }

    private fun nowText(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date())
    }

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
}
