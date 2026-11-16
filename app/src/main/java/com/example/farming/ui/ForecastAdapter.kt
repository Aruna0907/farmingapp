package com.example.farming.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.farming.R
import com.example.farming.data.model.ForecastDay
import com.example.farming.data.repository.RecommendationStatus
import com.example.farming.databinding.ItemForecastDayBinding
import java.util.Locale

class ForecastAdapter : ListAdapter<ForecastDay, ForecastAdapter.ViewHolder>(ForecastDiffCallback()) {

    class ViewHolder(val binding: ItemForecastDayBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemForecastDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context

        holder.binding.tvDayLabel.text = item.dayLabel
        holder.binding.tvTemps.text = String.format(Locale.getDefault(), "%.0f° / %.0f°C", item.tempMax, item.tempMin)
        holder.binding.tvEt0AndRain.text = String.format(Locale.getDefault(), "ET: %.1fmm | Rain: %.1fmm", item.et0, item.rainfall)

        if (item.status == RecommendationStatus.IRRIGATE) {
            holder.binding.tvStatus.text = "Irrigate"
            holder.binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_irrigate))
        } else {
            holder.binding.tvStatus.text = "Not needed"
            holder.binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_not_needed))
        }
    }

    private class ForecastDiffCallback : DiffUtil.ItemCallback<ForecastDay>() {
        override fun areItemsTheSame(oldItem: ForecastDay, newItem: ForecastDay): Boolean {
            return oldItem.dayLabel == newItem.dayLabel
        }

        override fun areContentsTheSame(oldItem: ForecastDay, newItem: ForecastDay): Boolean {
            return oldItem == newItem
        }
    }
}
