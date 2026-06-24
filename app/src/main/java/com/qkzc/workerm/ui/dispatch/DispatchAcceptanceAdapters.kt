package com.qkzc.workerm.ui.dispatch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.qkzc.workerm.data.dispatch.DispatchAcceptanceProcessCheck
import com.qkzc.workerm.data.dispatch.DispatchAcceptanceWorker
import com.qkzc.workerm.data.dispatch.DispatchEvidenceGroup
import com.qkzc.workerm.data.dispatch.DispatchStatusPresentation
import com.qkzc.workerm.databinding.ItemDispatchAcceptanceProcessBinding
import com.qkzc.workerm.databinding.ItemDispatchAcceptanceWorkerBinding
import com.qkzc.workerm.databinding.ItemDispatchEvidenceGroupBinding

class DispatchWorkerResultAdapter(
    private val onPhotos: (List<String>) -> Unit,
) : RecyclerView.Adapter<DispatchWorkerResultAdapter.Holder>() {
    private val items = mutableListOf<DispatchAcceptanceWorker>()
    fun submit(values: List<DispatchAcceptanceWorker>) { items.clear(); items.addAll(values); notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemDispatchAcceptanceWorkerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    inner class Holder(private val binding: ItemDispatchAcceptanceWorkerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DispatchAcceptanceWorker) {
            binding.nameText.text = item.name.ifBlank { "未命名工人" }
            binding.resultText.text = item.resultRemark.orEmpty().ifBlank { DispatchStatusPresentation.label(item.status) }
            binding.photoCountText.text = "图片 ${item.photoCount}张 ›"
            binding.root.setOnClickListener { if (item.photoUrls.isNotEmpty()) onPhotos(item.photoUrls) }
        }
    }
}

class DispatchProcessCheckAdapter(
    private val onPhotos: (List<String>) -> Unit,
) : RecyclerView.Adapter<DispatchProcessCheckAdapter.Holder>() {
    private val items = mutableListOf<DispatchAcceptanceProcessCheck>()
    fun submit(values: List<DispatchAcceptanceProcessCheck>) { items.clear(); items.addAll(values); notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemDispatchAcceptanceProcessBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position], position)
    inner class Holder(private val binding: ItemDispatchAcceptanceProcessBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DispatchAcceptanceProcessCheck, position: Int) {
            binding.indexText.text = (position + 1).toString()
            binding.nodeText.text = item.nodeName.ifBlank { "过程节点" }
            binding.reviewerText.text = "检查人：${item.reviewerName.orEmpty().ifBlank { "待检查" }}  ${item.checkTime.orEmpty()}"
            binding.statusText.text = DispatchStatusPresentation.label(item.status)
            binding.photoCountText.text = "${item.photoCount}张 ›"
            binding.root.setOnClickListener { if (item.photoUrls.isNotEmpty()) onPhotos(item.photoUrls) }
        }
    }
}

class DispatchEvidenceGroupAdapter(
    private val onPhotos: (List<String>) -> Unit,
) : RecyclerView.Adapter<DispatchEvidenceGroupAdapter.Holder>() {
    private val items = mutableListOf<DispatchEvidenceGroup>()
    fun submit(values: List<DispatchEvidenceGroup>) { items.clear(); items.addAll(values); notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemDispatchEvidenceGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    inner class Holder(private val binding: ItemDispatchEvidenceGroupBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DispatchEvidenceGroup) {
            binding.titleText.text = "${item.title}  ${item.count}张"
            binding.coverImage.load(item.previewUrls.firstOrNull()) { crossfade(true) }
            binding.root.setOnClickListener { if (item.previewUrls.isNotEmpty()) onPhotos(item.previewUrls) }
        }
    }
}
