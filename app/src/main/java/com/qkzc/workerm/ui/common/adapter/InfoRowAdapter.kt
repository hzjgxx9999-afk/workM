package com.qkzc.workerm.ui.common.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.qkzc.workerm.databinding.ItemInfoRowBinding
import com.qkzc.workerm.ui.model.InfoRowUi

class InfoRowAdapter(
    private val items: List<InfoRowUi>,
) : RecyclerView.Adapter<InfoRowAdapter.InfoRowViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoRowViewHolder {
        val binding = ItemInfoRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return InfoRowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InfoRowViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class InfoRowViewHolder(
        private val binding: ItemInfoRowBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InfoRowUi) {
            binding.titleText.text = item.title
            binding.subtitleText.text = item.subtitle
            binding.tagText.isVisible = !item.tag.isNullOrBlank()
            binding.trailingText.isVisible = !item.trailing.isNullOrBlank()
            binding.tagText.text = item.tag
            binding.trailingText.text = item.trailing
        }
    }
}
