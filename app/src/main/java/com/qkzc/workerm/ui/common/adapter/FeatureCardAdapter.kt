package com.qkzc.workerm.ui.common.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qkzc.workerm.databinding.ItemFeatureCardBinding
import com.qkzc.workerm.ui.model.FeatureCardUi

class FeatureCardAdapter(
    private val items: List<FeatureCardUi>,
) : RecyclerView.Adapter<FeatureCardAdapter.FeatureCardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureCardViewHolder {
        val binding = ItemFeatureCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return FeatureCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeatureCardViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class FeatureCardViewHolder(
        private val binding: ItemFeatureCardBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FeatureCardUi) {
            binding.featureTitle.text = item.title
            binding.featureDescription.text = item.description
            binding.featurePhase.text = item.phase
        }
    }
}
