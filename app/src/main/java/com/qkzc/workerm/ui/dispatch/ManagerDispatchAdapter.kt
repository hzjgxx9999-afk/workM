package com.qkzc.workerm.ui.dispatch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.qkzc.workerm.R
import com.qkzc.workerm.data.dispatch.DispatchOrder
import com.qkzc.workerm.data.dispatch.DispatchStatusPresentation

class ManagerDispatchAdapter(private val onClick: (DispatchOrder) -> Unit) : RecyclerView.Adapter<ManagerDispatchAdapter.Holder>() {
    private val items = mutableListOf<DispatchOrder>()
    fun submitList(value: List<DispatchOrder>) { items.clear(); items.addAll(value); notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_dispatch_order, parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position], onClick)
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val priority = view.findViewById<TextView>(R.id.dispatch_priority)
        private val title = view.findViewById<TextView>(R.id.dispatch_title)
        private val status = view.findViewById<TextView>(R.id.dispatch_status)
        private val meta = view.findViewById<TextView>(R.id.dispatch_meta)
        private val deadline = view.findViewById<TextView>(R.id.dispatch_deadline)
        fun bind(item: DispatchOrder, click: (DispatchOrder) -> Unit) {
            priority.text = when (item.priority) { "URGENT" -> "紧急"; "HIGH" -> "高"; "LOW" -> "低"; else -> "普通" }
            title.text = item.title
            status.text = DispatchStatusPresentation.label(item.status, item.overdue)
            meta.text = "${item.projectName} · ${item.leaderName}"
            deadline.text = "截止时间  ${item.deadlineTime}"
            itemView.setOnClickListener { click(item) }
        }
    }
}
