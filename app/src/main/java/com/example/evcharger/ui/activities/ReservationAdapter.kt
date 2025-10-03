package com.example.evcharger.ui.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.databinding.ItemReservationBinding
import com.example.evcharger.model.Reservation

/**
 * Adapter for reservations list (upcoming + history).
 */
class ReservationAdapter : ListAdapter<Reservation, ReservationAdapter.VH>(DIFF) {

    object DIFF : DiffUtil.ItemCallback<Reservation>() {
        override fun areItemsTheSame(oldItem: Reservation, newItem: Reservation) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Reservation, newItem: Reservation) = oldItem == newItem
    }

    inner class VH(val binding: ItemReservationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemReservationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = getItem(position)
        holder.binding.txtTitle.text = "Reservation ${r.id ?: ""} - ${r.stationName ?: r.stationId}"
        holder.binding.txtSubtitle.text = "Start: ${r.startTime} | Status: ${r.status}"
    }
}