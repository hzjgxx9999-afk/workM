package com.qkzc.workerm.ui.common.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qkzc.workerm.databinding.ItemMetricCardBinding
import com.qkzc.workerm.ui.model.DashboardMetricUi

class MetricAdapter(
    private val items: List<DashboardMetricUi>,
) : RecyclerView.Adapter<MetricAdapter.MetricViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetricViewHolder {
        val binding = ItemMetricCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return MetricViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MetricViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class MetricViewHolder(
        private val binding: ItemMetricCardBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DashboardMetricUi) {
            binding.metricCard.setCardBackgroundColor(Color.parseColor(item.backgroundColor))
            binding.metricLabel.text = item.label
            binding.metricValue.text = item.value
            binding.metricTrend.text = item.trend
        }
    }
}
