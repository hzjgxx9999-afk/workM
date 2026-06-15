package com.qkzc.workerm.ui.project

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.data.project.ConstructionLogAttachment
import com.qkzc.workerm.data.project.ConstructionLogDraft
import com.qkzc.workerm.data.project.ConstructionLogRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityConstructionLogEditBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ConstructionLogEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConstructionLogEditBinding
    private lateinit var sessionStore: SessionStore
    private val repository = ConstructionLogRepository()
    private val attachmentAdapter = ConstructionLogAttachmentAdapter(
        editable = true,
        onRemoveClick = ::removeAttachment,
    )

    private var currentProjectId: Long = 0L
    private var currentLogId: Long = 0L
    private var currentVersion: Int? = null
    private val uploadedAttachments = mutableListOf<ConstructionLogAttachment>()

    private val attachmentPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        uploadAttachments(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityConstructionLogEditBinding.inflate(layoutInflater)
        sessionStore = SessionStore(applicationContext)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.editRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        binding.dateInput.setOnClickListener { pickDate() }
        binding.addAttachmentButton.setOnClickListener { attachmentPicker.launch(arrayOf("*/*")) }
        binding.saveButton.setOnClickListener { saveDraft(submitAfterSave = false) }
        binding.submitButton.setOnClickListener { saveDraft(submitAfterSave = true) }
        binding.attachmentRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.attachmentRecyclerView.adapter = attachmentAdapter
        currentProjectId = intent.getLongExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, 0L)
        currentLogId = intent.getLongExtra(EXTRA_LOG_ID, 0L)
        if (currentLogId > 0L) {
            loadDetail()
        } else {
            binding.dateInput.setText(todayText())
        }
    }

    private fun loadDetail() {
        lifecycleScope.launch {
            runCatching {
                val session = sessionStore.sessionFlow.first()
                val projectId = currentProjectId.takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                repository.detail(session.accessToken, projectId, currentLogId)
            }.onSuccess { detail ->
                currentVersion = detail.version
                binding.dateInput.setText(detail.logDate)
                binding.sectionInput.setText(detail.sectionName)
                binding.teamInput.setText(detail.teamName)
                binding.titleInput.setText(detail.title)
                binding.weatherAmInput.setText(detail.weatherAm)
                binding.weatherPmInput.setText(detail.weatherPm)
                binding.workerCountInput.setText(detail.workerCount.toString())
                binding.machineCountInput.setText(detail.machineCount.toString())
                binding.progressInput.setText(detail.progressSummary)
                binding.qualityInput.setText(detail.qualitySummary)
                binding.safetyInput.setText(detail.safetySummary)
                binding.contentInput.setText(detail.contentText)
                binding.riskInput.setText(detail.riskCount.toString())
                binding.locationInput.setText(detail.locationText)
                binding.rectifyInput.setText(detail.rectifySuggestion)
                binding.remarkInput.setText(detail.extraRemark)
                uploadedAttachments.clear()
                uploadedAttachments.addAll(detail.attachments)
                attachmentAdapter.submitList(uploadedAttachments.toList())
            }.onFailure { throwable ->
                Toast.makeText(this@ConstructionLogEditActivity, throwable.message ?: "日志详情加载失败", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun saveDraft(submitAfterSave: Boolean) {
        val draft = buildDraft() ?: return
        lifecycleScope.launch {
            binding.saveButton.isEnabled = false
            binding.submitButton.isEnabled = false
            runCatching {
                val session = sessionStore.sessionFlow.first()
                val projectId = currentProjectId.takeIf { it > 0L }
                    ?: session.projectId.toLongOrNull()
                    ?: error("当前账号没有可管理项目")
                val detail = if (currentLogId > 0L) {
                    repository.update(session.accessToken, projectId, currentLogId, draft.copy(version = currentVersion))
                } else {
                    repository.create(session.accessToken, projectId, draft)
                }
                currentLogId = detail.id
                currentVersion = detail.version
                if (submitAfterSave) {
                    repository.submit(session.accessToken, projectId, currentLogId, detail.version)
                }
                detail
            }.onSuccess {
                Toast.makeText(
                    this@ConstructionLogEditActivity,
                    if (submitAfterSave) "施工日志已提交审核" else "施工日志草稿已保存",
                    Toast.LENGTH_SHORT,
                ).show()
                finish()
            }.onFailure { throwable ->
                Toast.makeText(
                    this@ConstructionLogEditActivity,
                    throwable.message ?: if (submitAfterSave) "提交失败" else "保存失败",
                    Toast.LENGTH_SHORT,
                ).show()
            }
            binding.saveButton.isEnabled = true
            binding.submitButton.isEnabled = true
        }
    }

    private fun buildDraft(): ConstructionLogDraft? {
        val logDate = binding.dateInput.text?.toString().orEmpty().trim()
        val title = binding.titleInput.text?.toString().orEmpty().trim()
        val progress = binding.progressInput.text?.toString().orEmpty().trim()
        val quality = binding.qualityInput.text?.toString().orEmpty().trim()
        val safety = binding.safetyInput.text?.toString().orEmpty().trim()
        val content = binding.contentInput.text?.toString().orEmpty().trim()
        if (logDate.isBlank()) {
            toast("请选择日志日期")
            return null
        }
        if (title.isBlank()) {
            toast("请输入日志标题")
            return null
        }
        if (progress.isBlank() || quality.isBlank() || safety.isBlank() || content.isBlank()) {
            toast("请补全施工内容、质量、安全和记录内容")
            return null
        }
        return ConstructionLogDraft(
            logDate = logDate,
            sectionName = binding.sectionInput.text?.toString().orEmpty().trim(),
            teamName = binding.teamInput.text?.toString().orEmpty().trim(),
            title = title,
            weatherAm = binding.weatherAmInput.text?.toString().orEmpty().trim(),
            weatherPm = binding.weatherPmInput.text?.toString().orEmpty().trim(),
            workerCount = binding.workerCountInput.text?.toString().orEmpty().toIntOrNull() ?: 0,
            machineCount = binding.machineCountInput.text?.toString().orEmpty().toIntOrNull() ?: 0,
            progressSummary = progress,
            qualitySummary = quality,
            safetySummary = safety,
            contentText = content,
            riskCount = binding.riskInput.text?.toString().orEmpty().toIntOrNull() ?: 0,
            locationText = binding.locationInput.text?.toString().orEmpty().trim(),
            rectifySuggestion = binding.rectifyInput.text?.toString().orEmpty().trim(),
            extraRemark = binding.remarkInput.text?.toString().orEmpty().trim(),
            attachments = uploadedAttachments.toList(),
            version = currentVersion,
        )
    }

    private fun uploadAttachments(uris: List<Uri>) {
        lifecycleScope.launch {
            binding.addAttachmentButton.isEnabled = false
            runCatching {
                val session = sessionStore.sessionFlow.first()
                uris.forEach { uri ->
                    val fileName = queryFileName(uri)
                    val mimeType = contentResolver.getType(uri)
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("文件读取失败")
                    val uploaded = repository.uploadFile(
                        token = session.accessToken,
                        fileName = fileName,
                        mimeType = mimeType,
                        bytes = bytes,
                    )
                    uploadedAttachments.add(uploaded)
                }
            }.onSuccess {
                attachmentAdapter.submitList(uploadedAttachments.toList())
                toast("附件上传完成")
            }.onFailure { throwable ->
                toast(throwable.message ?: "附件上传失败")
            }
            binding.addAttachmentButton.isEnabled = true
        }
    }

    private fun queryFileName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex).orEmpty().ifBlank { "attachment_${System.currentTimeMillis()}" }
            }
        }
        return "attachment_${System.currentTimeMillis()}"
    }

    private fun removeAttachment(attachment: ConstructionLogAttachment) {
        uploadedAttachments.remove(attachment)
        attachmentAdapter.submitList(uploadedAttachments.toList())
    }

    private fun pickDate() {
        val calendar = Calendar.getInstance()
        val existing = binding.dateInput.text?.toString().orEmpty()
        if (existing.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            runCatching {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(existing) ?: Date()
                calendar.time = parsed
            }
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                binding.dateInput.setText(String.format(Locale.CHINA, "%04d-%02d-%02d", year, month + 1, day))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun todayText(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_LOG_ID = "logId"
    }
}
