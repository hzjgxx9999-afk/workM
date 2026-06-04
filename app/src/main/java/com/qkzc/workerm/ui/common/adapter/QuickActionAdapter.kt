package com.qkzc.workerm.ui.common.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qkzc.workerm.databinding.ItemQuickActionBinding
import com.qkzc.workerm.ui.model.QuickActionUi

class QuickActionAdapter(
    private val items: List<QuickActionUi>,
    private val onItemClick: (QuickActionUi) -> Unit = {},
) : RecyclerView.Adapter<QuickActionAdapter.QuickActionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickActionViewHolder {
        val binding = ItemQuickActionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return QuickActionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuickActionViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class QuickActionViewHolder(
        private val binding: ItemQuickActionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuickActionUi, onItemClick: (QuickActionUi) -> Unit) {
            binding.iconContainer.setCardBackgroundColor(Color.parseColor(item.accentColor))
            binding.iconView.setImageResource(item.iconRes)
            binding.actionText.text = item.title
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
