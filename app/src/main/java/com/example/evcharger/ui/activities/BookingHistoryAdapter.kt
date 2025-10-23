package com.example.evcharger.ui.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.R
import com.example.evcharger.databinding.ItemBookingHistoryBinding
import com.example.evcharger.model.BookingResponseData

class BookingHistoryAdapter : ListAdapter<BookingResponseData, BookingHistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookingHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemBookingHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: BookingResponseData) {
            binding.apply {
                // Station info
                tvStationName.text = booking.stationName ?: root.context.getString(R.string.booking_unknown_station)
                tvLocation.text = booking.stationLocation ?: root.context.getString(R.string.location_not_available)

                // Booking details
                tvBookingId.text = booking.bookingId
                tvSlot.text = root.context.getString(R.string.slot_format, booking.slotNumber)
                tvDateTime.text = booking.reservationDateTime ?: root.context.getString(R.string.not_specified)
                tvDuration.text = root.context.getString(R.string.duration_hours, booking.duration)

                // Status badge
                val status = booking.status ?: "Unknown"
                tvStatus.text = status.uppercase()

                // Set status color
                val statusColor = when (status.lowercase()) {
                    "completed" -> R.color.success
                    "approved" -> R.color.primary
                    "cancelled" -> R.color.error
                    "pending" -> R.color.warning
                    else -> R.color.text_secondary
                }
                statusBadge.setCardBackgroundColor(
                    ContextCompat.getColor(root.context, statusColor)
                )
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BookingResponseData>() {
        override fun areItemsTheSame(
            oldItem: BookingResponseData,
            newItem: BookingResponseData
        ): Boolean {
            return oldItem.bookingId == newItem.bookingId
        }

        override fun areContentsTheSame(
            oldItem: BookingResponseData,
            newItem: BookingResponseData
        ): Boolean {
            return oldItem == newItem
        }
    }
}
