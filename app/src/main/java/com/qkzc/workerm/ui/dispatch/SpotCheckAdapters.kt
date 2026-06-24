package com.qkzc.workerm.ui.dispatch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.qkzc.workerm.data.dispatch.DispatchLinkedProcessCheck
import com.qkzc.workerm.data.dispatch.DispatchSpotCheckPhoto
import com.qkzc.workerm.data.dispatch.DispatchStatusPresentation
import com.qkzc.workerm.databinding.ItemSpotCheckPhotoBinding
import com.qkzc.workerm.databinding.ItemSpotLinkedProcessBinding

class SpotCheckPhotoAdapter(private val onClick: (Int) -> Unit) : RecyclerView.Adapter<SpotCheckPhotoAdapter.Holder>() {
    private val items = mutableListOf<DispatchSpotCheckPhoto>()
    fun submit(values: List<DispatchSpotCheckPhoto>) { items.clear(); items.addAll(values); notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemSpotCheckPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position], position)
    inner class Holder(private val binding: ItemSpotCheckPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DispatchSpotCheckPhoto, position: Int) {
            binding.photoImage.load(item.previewUrl) { crossfade(true) }
            binding.root.setOnClickListener { onClick(position) }
        }
    }
}

class SpotLinkedProcessAdapter : RecyclerView.Adapter<SpotLinkedProcessAdapter.Holder>() {
    private val items = mutableListOf<DispatchLinkedProcessCheck>()
    fun submit(values: List<DispatchLinkedProcessCheck>) { items.clear(); items.addAll(values); notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemSpotLinkedProcessBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    class Holder(private val binding: ItemSpotLinkedProcessBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DispatchLinkedProcessCheck) {
            binding.nameText.text = item.nodeName.ifBlank { "过程节点" }
            binding.timeText.text = item.checkTime.orEmpty()
            binding.statusText.text = DispatchStatusPresentation.label(item.status)
        }
    }
}
