package com.qkzc.workerm.ui.dispatch

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qkzc.workerm.R
import com.qkzc.workerm.data.dispatch.DispatchRepository
import com.qkzc.workerm.data.dispatch.DispatchSpotCheckDetail
import com.qkzc.workerm.data.dispatch.DispatchStatusPresentation
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivitySpotCheckReviewBinding
import com.qkzc.workerm.ui.common.EdgeToEdgeActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SpotCheckReviewActivity : EdgeToEdgeActivity() {
    private lateinit var binding: ActivitySpotCheckReviewBinding
    private val repository = DispatchRepository()
    private val sessionStore by lazy { SessionStore(applicationContext) }
    private val photoAdapter = SpotCheckPhotoAdapter { openPhoto(it) }
    private val processAdapter = SpotLinkedProcessAdapter()
    private var detail = DispatchSpotCheckDetail()
    private val orderId by lazy { intent.getLongExtra(EXTRA_ORDER_ID, 0L) }
    private val nodeId by lazy { intent.getLongExtra(EXTRA_NODE_ID, 0L) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpotCheckReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdge(
            root = binding.root,
            topBarId = R.id.spot_check_top_bar,
            bottomBarId = R.id.review_action_bar,
        )
        binding.photoList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.photoList.adapter = photoAdapter
        binding.linkedProcessList.layoutManager = LinearLayoutManager(this)
        binding.linkedProcessList.adapter = processAdapter
        binding.backButton.setOnClickListener { finish() }
        binding.operationRecordButton.setOnClickListener { showOperations() }
        binding.rejectButton.setOnClickListener { reviewSpotCheck("REJECTED") }
        binding.passButton.setOnClickListener { reviewSpotCheck("PASSED") }
        load()
    }

    private fun load() = lifecycleScope.launch {
        runCatching {
            val token = sessionStore.sessionFlow.first().accessToken
            repository.spotCheckDetail(token, orderId, nodeId)
        }.onSuccess { detail = it; render() }
            .onFailure { toast(it.message ?: "抽查详情加载失败") }
    }

    private fun render() = with(binding) {
        val submission = detail.latestSubmission
        spotStatusText.text = DispatchStatusPresentation.label(detail.node.status)
        titleText.text = detail.order.title
        phaseText.text = when (detail.node.status) {
            "WAIT_SUBMIT" -> "等待班组长提交抽查资料"
            "WAIT_CHECK" -> "抽查资料已提交，等待审核"
            "REJECTED" -> "已驳回，等待重新提交"
            "PASSED" -> "抽查审核已通过"
            else -> DispatchStatusPresentation.label(detail.node.status)
        }
        orderInfoText.text = "项目：${detail.order.projectName}\n班组：${detail.order.teamName}\n班组长：${detail.order.leaderName}\n施工位置：${detail.order.locationDesc}\n抽查节点：${detail.node.nodeName}\n抽查时限：${detail.node.submitDeadline.orEmpty().ifBlank { "未设置" }}"
        submissionMetaText.text = "提交时间：${submission?.submitTime.orEmpty().ifBlank { "尚未提交" }}    提交人：${submission?.submitterName.orEmpty().ifBlank { detail.order.leaderName }}"
        descriptionText.text = submission?.description.orEmpty().ifBlank { detail.node.description.orEmpty().ifBlank { "暂无抽查说明" } }
        locationText.text = "定位水印：${submission?.address.orEmpty().ifBlank { "未采集定位" }}"
        photoAdapter.submit(detail.photos)
        processAdapter.submit(detail.linkedProcessChecks)
        reviewSection.visibility = if (detail.canReview) View.VISIBLE else View.GONE
        reviewActionBar.visibility = if (detail.canReview) View.VISIBLE else View.GONE
    }

    private fun reviewSpotCheck(result: String) {
        val comment = binding.reviewInput.text?.toString()?.trim().orEmpty()
        if (comment.isBlank()) { binding.reviewInput.error = "请填写抽查审核意见"; return }
        if (comment.length > MAX_REVIEW_LENGTH) { binding.reviewInput.error = "审核意见不能超过200字"; return }
        val submission = detail.latestSubmission ?: return
        binding.rejectButton.isEnabled = false
        binding.passButton.isEnabled = false
        lifecycleScope.launch {
            runCatching {
                val token = sessionStore.sessionFlow.first().accessToken
                repository.reviewProcessRecord(token, submission.recordId, result, comment, submission.version)
            }.onSuccess {
                setResult(RESULT_OK)
                toast(if (result == "PASSED") "抽查已通过" else "已驳回整改")
                load()
            }.onFailure { toast(it.message ?: "抽查审核失败") }
            binding.rejectButton.isEnabled = true
            binding.passButton.isEnabled = true
        }
    }

    private fun showOperations() {
        val lines = detail.operations.map {
            "${it.createTime.orEmpty()}  ${it.operatorName}\n${operationLabel(it.action)}${it.remark?.let { text -> "：$text" }.orEmpty()}"
        }
        AlertDialog.Builder(this).setTitle("抽查操作记录")
            .setItems(lines.ifEmpty { listOf("暂无操作记录") }.toTypedArray(), null)
            .setPositiveButton("关闭", null).show()
    }

    private fun operationLabel(action: String) = when (action) {
        "CREATE_SPOT_CHECK" -> "发起抽查"; "SUBMIT_SPOT_CHECK" -> "提交抽查资料"
        "PROCESS_REVIEW_PASS" -> "抽查通过"; "PROCESS_REVIEW_REJECT" -> "驳回整改"
        "CANCEL_SPOT_CHECK" -> "取消抽查"; else -> action
    }

    private fun openPhoto(index: Int) = startActivity(DispatchPhotoPreviewActivity.intent(this, detail.photos.map { it.previewUrl }, index))
    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    companion object {
        const val MAX_REVIEW_LENGTH = 200
        private const val EXTRA_ORDER_ID = "order_id"
        private const val EXTRA_NODE_ID = "node_id"
        fun intent(context: Context, orderId: Long, nodeId: Long) = Intent(context, SpotCheckReviewActivity::class.java)
            .putExtra(EXTRA_ORDER_ID, orderId).putExtra(EXTRA_NODE_ID, nodeId)
    }
}
