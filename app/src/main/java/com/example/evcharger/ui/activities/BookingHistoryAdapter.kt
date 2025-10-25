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
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class BookingHistoryAdapter(
    private val onItemClick: (BookingResponseData) -> Unit
) : ListAdapter<BookingResponseData, BookingHistoryAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardBooking)
        val txtBookingId: TextView = view.findViewById(R.id.txtBookingId)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        val txtCustomer: TextView? = view.findViewById(R.id.txtCustomer)
        val txtStationName: TextView? = view.findViewById(R.id.txtStationName)
        val txtSlot: TextView = view.findViewById(R.id.txtSlot)
        val txtReservationTime: TextView = view.findViewById(R.id.txtReservationTime)
        val txtDuration: TextView = view.findViewById(R.id.txtDuration)
        val txtCompletedTime: TextView? = view.findViewById(R.id.txtCompletedTime)
        val txtCancelledTime: TextView? = view.findViewById(R.id.txtCancelledTime)
        val txtEnergyConsumed: TextView? = view.findViewById(R.id.txtEnergyConsumed)
        val txtCost: TextView? = view.findViewById(R.id.txtCost)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).status?.lowercase()) {
            "completed" -> VIEW_TYPE_COMPLETED
            "cancelled" -> VIEW_TYPE_CANCELLED
            else -> VIEW_TYPE_COMPLETED // Default to completed layout
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_COMPLETED -> R.layout.item_booking_history_completed
            VIEW_TYPE_CANCELLED -> R.layout.item_booking_history_cancelled
            else -> R.layout.item_booking_history_completed
        }
        
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = getItem(position)
        
        holder.txtBookingId.text = booking.bookingId
        holder.txtStatus.text = booking.status ?: "Unknown"
        holder.txtStationName?.text = booking.stationName ?: "Unknown Station"
        holder.txtSlot.text = "⚡ Slot ${booking.slotNumber ?: "N/A"}"
        
        val reservationTime = formatDateTime(booking.reservationDateTime)
        holder.txtReservationTime.text = "📅 Scheduled: $reservationTime"
        
        holder.txtDuration.text = "⏱️ Duration: ${booking.duration ?: 0} hour(s)"

        // Status-specific fields
        when (booking.status?.lowercase()) {
            "completed" -> {
                val completedTime = formatDateTime(booking.completedAt)
                holder.txtCompletedTime?.text = "🕒 Completed: $completedTime"
                
                holder.txtEnergyConsumed?.text = "${booking.energyConsumed ?: 0} kWh"
                holder.txtCost?.text = "LKR ${String.format("%.2f", booking.cost ?: 0.0)}"
            }
            "cancelled" -> {
                val cancelledTime = formatDateTime(booking.cancelledAt)
                holder.txtCancelledTime?.text = "🕒 Cancelled: $cancelledTime"
            }
        }

        // Set card click listener
        holder.itemView.setOnClickListener {
            onItemClick(booking)
        }

        // Set card colors based on status
        val strokeColor = when (booking.status?.lowercase()) {
            "completed" -> holder.itemView.context.getColor(R.color.primary)
            "cancelled" -> holder.itemView.context.getColor(R.color.error)
            else -> holder.itemView.context.getColor(R.color.divider)
        }
        holder.card.strokeColor = strokeColor
    }

    private fun formatDateTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "N/A"

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(isoString)

            val outputFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            isoString
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

    companion object {
        private const val VIEW_TYPE_COMPLETED = 1
        private const val VIEW_TYPE_CANCELLED = 2
    }
}
