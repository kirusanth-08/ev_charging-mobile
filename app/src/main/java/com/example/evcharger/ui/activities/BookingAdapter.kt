package com.example.evcharger.ui.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.R
import com.example.evcharger.model.BookingResponseData
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Adapter for displaying BookingResponseData (pending bookings)
 */
class BookingAdapter(
    private val onBookingClick: ((BookingResponseData) -> Unit)? = null
) : ListAdapter<BookingResponseData, BookingAdapter.ViewHolder>(BookingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtBookingId: TextView = itemView.findViewById(R.id.txtBookingId)
        private val txtStationName: TextView = itemView.findViewById(R.id.txtStationName)
        private val txtStationLocation: TextView = itemView.findViewById(R.id.txtStationLocation)
        private val txtSlotNumber: TextView = itemView.findViewById(R.id.txtSlotNumber)
        private val txtDateTime: TextView = itemView.findViewById(R.id.txtDateTime)
        private val txtDuration: TextView = itemView.findViewById(R.id.txtDuration)
        private val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        private val txtTimeUntil: TextView = itemView.findViewById(R.id.txtTimeUntil)

        fun bind(booking: BookingResponseData) {
            txtBookingId.text = "Booking ID: ${booking.bookingId}"
            txtStationName.text = booking.stationName ?: "Unknown Station"
            txtStationLocation.text = booking.stationLocation ?: ""
            txtSlotNumber.text = "Slot ${booking.slotNumber ?: "?"}"
            
            // Format reservation date time
            booking.reservationDateTime?.let { dateTime ->
                try {
                    val zonedDateTime = ZonedDateTime.parse(dateTime)
                    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")
                    txtDateTime.text = zonedDateTime.format(formatter)
                } catch (e: Exception) {
                    txtDateTime.text = dateTime
                }
            } ?: run {
                txtDateTime.text = "No date set"
            }
            
            txtDuration.text = "${booking.duration ?: 0} hours"
            
            // Status with color
            val status = booking.status ?: "Unknown"
            txtStatus.text = status
            when (status.lowercase()) {
                "pending" -> {
                    txtStatus.setTextColor(itemView.context.getColor(R.color.ev_busy))
                    txtStatus.setBackgroundResource(R.drawable.status_pending_bg)
                }
                "approved" -> {
                    txtStatus.setTextColor(itemView.context.getColor(R.color.ev_available))
                    txtStatus.setBackgroundResource(R.drawable.status_approved_bg)
                }
                "cancelled" -> {
                    txtStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                    txtStatus.setBackgroundResource(R.drawable.status_cancelled_bg)
                }
                else -> {
                    txtStatus.setTextColor(itemView.context.getColor(R.color.text_secondary))
                }
            }
            
            // Time until reservation
            txtTimeUntil.text = booking.timeUntilReservation ?: ""
            txtTimeUntil.visibility = if (booking.timeUntilReservation.isNullOrBlank()) {
                View.GONE
            } else {
                View.VISIBLE
            }
            
            // Click listener
            itemView.setOnClickListener {
                onBookingClick?.invoke(booking)
            }
        }
    }

    class BookingDiffCallback : DiffUtil.ItemCallback<BookingResponseData>() {
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
