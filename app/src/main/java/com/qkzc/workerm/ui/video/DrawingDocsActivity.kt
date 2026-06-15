package com.qkzc.workerm.ui.video

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.R
import com.qkzc.workerm.data.project.DrawingDoc
import com.qkzc.workerm.data.project.DrawingDocsRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityDrawingDocsBinding
import com.qkzc.workerm.ui.cad.CadPreviewActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DrawingDocsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawingDocsBinding
    private val repository = DrawingDocsRepository()
    private lateinit var sessionStore: SessionStore
    private var remoteDrawingDocs: List<DrawingDoc> = emptyList()
    private val uploadedDrawingDocs = mutableListOf<DrawingDoc>()
    private var drawingDocs: List<DrawingDoc> = emptyList()
    private var currentDoc: DrawingDoc? = null
    private var currentProjectId: Long = 0L
    private var currentToken: String = ""
    private val pickDrawingLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            uploadSelectedDrawing(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sessionStore = SessionStore(this)
        binding = ActivityDrawingDocsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawingDocsRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        bindDocSelection()
        loadDrawingDocs()
    }

    private fun bindDocSelection() {
        binding.previewCard.setOnClickListener { openCurrentDoc() }
        binding.openCadPreviewButton.setOnClickListener { openCurrentDoc() }
        binding.uploadDrawingButton.setOnClickListener {
            pickDrawingLauncher.launch(arrayOf("*/*"))
        }
    }

    private fun loadDrawingDocs() {
        lifecycleScope.launch {
            binding.uploadDrawingButton.isEnabled = false
            binding.previewHintText.text = "正在加载项目图纸..."
            runCatching {
                val session = sessionStore.sessionFlow.first()
                currentToken = session.accessToken
                currentProjectId = intent.getLongExtra(EXTRA_PROJECT_ID, 0L)
                    .takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("缺少项目 ID，无法加载图纸")
                repository.loadDrawingDocs(currentToken, currentProjectId)
            }.onSuccess { docs ->
                remoteDrawingDocs = docs
                refreshDrawingDocs(selectFirst = currentDoc == null)
            }.onFailure { throwable ->
                Toast.makeText(this@DrawingDocsActivity, throwable.message ?: "图纸列表加载失败", Toast.LENGTH_SHORT).show()
                refreshDrawingDocs(selectFirst = false)
            }
            binding.uploadDrawingButton.isEnabled = true
        }
    }

    private fun refreshDrawingDocs(selectFirst: Boolean) {
        drawingDocs = (uploadedDrawingDocs + remoteDrawingDocs)
            .distinctBy { "${it.fileName}_${it.createTime}_${it.rawUrl}" }
        renderVersionList()
        val selected = currentDoc?.let { current ->
            drawingDocs.firstOrNull { it.docId == current.docId && it.fileName == current.fileName }
        }
        when {
            selected != null -> selectDoc(selected)
            selectFirst && drawingDocs.isNotEmpty() -> selectDoc(drawingDocs.first())
            drawingDocs.isEmpty() -> renderEmptyState()
        }
    }

    private fun selectDoc(doc: DrawingDoc) {
        currentDoc = doc
        binding.previewFileNameText.text = "${doc.fileName}    ${doc.displayType}    ${doc.createTime.ifBlank { "未知时间" }}"
        binding.previewStatusText.text = when {
            doc.supportsCadPreview && doc.canOpenPreview -> "可预览"
            doc.uploadedFromMobile -> "刚上传"
            else -> "仅列表"
        }
        binding.previewHintText.text = when {
            doc.supportsCadPreview && doc.canOpenPreview ->
                "点击下方按钮后，将在 App 内打开 CAD 在线预览页"
            doc.canOpenPreview ->
                "当前文件支持打开链接预览，但不是 CAD 图纸格式"
            else ->
                "后台当前未返回该文件的预览地址，请补充预览 URL 字段或专用预览接口"
        }
        binding.openCadPreviewButton.text = if (doc.canOpenPreview) {
            if (doc.supportsCadPreview) "打开 CAD 预览" else "打开文件预览"
        } else {
            "当前文件暂不可预览"
        }
        binding.openCadPreviewButton.isEnabled = doc.canOpenPreview
        binding.openCadPreviewButton.alpha = if (doc.canOpenPreview) 1f else 0.5f
        highlightSelectedVersion(doc)
    }

    private fun openCurrentDoc() {
        val doc = currentDoc ?: return
        if (!doc.canOpenPreview) {
            Toast.makeText(this, "后台暂未返回该文件的预览地址", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, CadPreviewActivity::class.java)
                .putExtra(CadPreviewActivity.EXTRA_TITLE, doc.fileName)
                .putExtra(CadPreviewActivity.EXTRA_PREVIEW_URL, doc.previewPageUrl),
        )
    }

    private fun renderVersionList() {
        binding.versionsContainer.removeAllViews()
        binding.versionsEmptyText.isVisible = drawingDocs.isEmpty()
        drawingDocs.forEachIndexed { index, doc ->
            binding.versionsContainer.addView(createVersionRow(doc, index))
        }
    }

    private fun createVersionRow(doc: DrawingDoc, index: Int): TextView {
        return TextView(this).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = if (index == 0) 0 else 10.dp()
            }
            minHeight = 58.dp()
            setPadding(14.dp(), 10.dp(), 14.dp(), 10.dp())
            background = getDrawable(R.drawable.bg_search_pill)
            setTextColor(getColor(R.color.text_primary))
            textSize = 13f
            isClickable = true
            isFocusable = true
            text = buildString {
                append(doc.fileName)
                append("  ")
                append(doc.displayType)
                append("\n")
                append(doc.uploadedBy.ifBlank { "未知上传人" })
                append("    ")
                append(doc.createTime.ifBlank { "未知时间" })
                doc.category.takeIf { it.isNotBlank() }?.let {
                    append("    ")
                    append(it)
                }
            }
            setOnClickListener { selectDoc(doc) }
            tag = doc.docId
        }
    }

    private fun highlightSelectedVersion(selected: DrawingDoc) {
        for (index in 0 until binding.versionsContainer.childCount) {
            val child = binding.versionsContainer.getChildAt(index)
            child.alpha = if (child.tag == selected.docId) 1f else 0.72f
        }
    }

    private fun renderEmptyState() {
        currentDoc = null
        binding.previewStatusText.text = "暂无图纸"
        binding.previewFileNameText.text = "当前项目暂无可用图纸文件"
        binding.previewHintText.text = "你可以点击下方“上传图纸”，从手机选择 DWG/DXF/PDF 文件上传"
        binding.openCadPreviewButton.text = "当前文件暂不可预览"
        binding.openCadPreviewButton.isEnabled = false
        binding.openCadPreviewButton.alpha = 0.5f
    }

    private fun uploadSelectedDrawing(uri: Uri) {
        lifecycleScope.launch {
            binding.uploadDrawingButton.isEnabled = false
            binding.uploadDrawingButton.text = "上传中..."
            runCatching {
                if (currentProjectId <= 0L || currentToken.isBlank()) {
                    val session = sessionStore.sessionFlow.first()
                    currentToken = session.accessToken
                    currentProjectId = intent.getLongExtra(EXTRA_PROJECT_ID, 0L)
                        .takeIf { it > 0L }
                        ?: session.projectId.toLongOrNull()
                        ?: error("缺少项目 ID，无法上传图纸")
                }
                val fileName = queryFileName(uri)
                validateDrawingType(fileName)
                val mimeType = contentResolver.getType(uri)
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("图纸读取失败")
                repository.uploadDrawing(
                    token = currentToken,
                    projectId = currentProjectId,
                    fileName = fileName,
                    mimeType = mimeType,
                    bytes = bytes,
                )
            }.onSuccess { uploaded ->
                uploadedDrawingDocs.removeAll { it.fileName == uploaded.fileName && it.rawUrl == uploaded.rawUrl }
                uploadedDrawingDocs.add(0, uploaded)
                refreshDrawingDocs(selectFirst = false)
                selectDoc(uploaded)
                remoteDrawingDocs = repository.loadDrawingDocs(currentToken, currentProjectId)
                refreshDrawingDocs(selectFirst = false)
                drawingDocs.firstOrNull { it.fileName == uploaded.fileName }
                    ?.let { selectDoc(it) }
                Toast.makeText(this@DrawingDocsActivity, "图纸上传完成", Toast.LENGTH_SHORT).show()
                if (uploaded.canOpenPreview) {
                    openCurrentDoc()
                }
            }.onFailure { throwable ->
                Toast.makeText(this@DrawingDocsActivity, throwable.message ?: "图纸上传失败", Toast.LENGTH_SHORT).show()
            }
            binding.uploadDrawingButton.isEnabled = true
            binding.uploadDrawingButton.text = "上传图纸"
        }
    }

    private fun validateDrawingType(fileName: String) {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext !in SUPPORTED_UPLOAD_TYPES) {
            error("仅支持上传 DWG、DXF、PDF 文件")
        }
    }

    private fun queryFileName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex).orEmpty().ifBlank { "drawing_${System.currentTimeMillis()}" }
            }
        }
        return "drawing_${System.currentTimeMillis()}"
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_PROJECT_ID = "projectId"
        private val SUPPORTED_UPLOAD_TYPES = setOf("dwg", "dxf", "pdf")
    }
}
