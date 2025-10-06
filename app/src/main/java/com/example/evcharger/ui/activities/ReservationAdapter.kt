package com.example.evcharger.ui.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.databinding.ItemReservationBinding
import com.example.evcharger.model.Reservation
import com.example.evcharger.util.QRCodeUtil
import android.graphics.Bitmap

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
        holder.binding.txtSlot.text = "Slot: ${r.slotNumber ?: "-"} | Duration: ${r.duration ?: "-"}h"
        holder.binding.txtTimeUntil.text = r.timeUntilReservation ?: ""
        holder.binding.txtApprovedInfo.text = r.approvedBy?.let { "Approved by: $it @ ${r.approvedAt ?: ""}" } ?: ""

        // Render small QR thumbnail if available
        val qrPayload = r.qrCode ?: r.qrCodePayload
        if (!qrPayload.isNullOrEmpty()) {
            try {
                val bmp: Bitmap = QRCodeUtil.generate(qrPayload, 128)
                holder.binding.imgQr.setImageBitmap(bmp)
            } catch (e: Exception) {
                holder.binding.imgQr.setImageResource(android.R.color.transparent)
            }
        } else {
            holder.binding.imgQr.setImageResource(android.R.color.transparent)
        }
    }
}