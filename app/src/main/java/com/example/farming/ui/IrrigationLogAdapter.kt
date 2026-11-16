package com.example.farming.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.farming.data.model.IrrigationLog
import com.example.farming.databinding.ItemIrrigationLogBinding

class IrrigationLogAdapter : ListAdapter<IrrigationLog, IrrigationLogAdapter.ViewHolder>(LogDiffCallback()) {

    class ViewHolder(val binding: ItemIrrigationLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIrrigationLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.tvLogTimestamp.text = item.timestamp
        holder.binding.tvLogDuration.text = "Duration: ${item.durationMinutes} mins"
    }

    private class LogDiffCallback : DiffUtil.ItemCallback<IrrigationLog>() {
        override fun areItemsTheSame(oldItem: IrrigationLog, newItem: IrrigationLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: IrrigationLog, newItem: IrrigationLog): Boolean {
            return oldItem == newItem
        }
    }
}
