package com.example.evcharger.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.R
import com.example.evcharger.model.BookingResponseData
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class StationBookingsAdapter(
    private var bookings: List<BookingResponseData>,
    private val onCompleteClick: (BookingResponseData) -> Unit,
    private val onApproveClick: (BookingResponseData) -> Unit
) : RecyclerView.Adapter<StationBookingsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardBooking)
        val txtBookingId: TextView = view.findViewById(R.id.txtBookingId)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        val txtCustomer: TextView = view.findViewById(R.id.txtCustomer)
        val txtSlot: TextView = view.findViewById(R.id.txtSlot)
        val txtReservationTime: TextView = view.findViewById(R.id.txtReservationTime)
        val txtDuration: TextView = view.findViewById(R.id.txtDuration)
        val txtTimeUntil: TextView? = view.findViewById(R.id.txtTimeUntil)
        val txtArrivalTime: TextView? = view.findViewById(R.id.txtArrivalTime)
        val txtCompletedTime: TextView? = view.findViewById(R.id.txtCompletedTime)
        val txtCancelledTime: TextView? = view.findViewById(R.id.txtCancelledTime)
        val txtEnergyConsumed: TextView? = view.findViewById(R.id.txtEnergyConsumed)
        val txtCost: TextView? = view.findViewById(R.id.txtCost)
        val btnApprove: MaterialButton? = view.findViewById(R.id.btnApprove)
        val btnComplete: MaterialButton? = view.findViewById(R.id.btnComplete)
    }

    override fun getItemViewType(position: Int): Int {
        return when (bookings[position].status?.lowercase()) {
            "pending" -> VIEW_TYPE_PENDING
            "approved" -> VIEW_TYPE_APPROVED
            "confirmed" -> VIEW_TYPE_CONFIRMED
            "completed" -> VIEW_TYPE_COMPLETED
            "cancelled" -> VIEW_TYPE_CANCELLED
            else -> VIEW_TYPE_PENDING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_PENDING -> R.layout.item_booking_pending
            VIEW_TYPE_APPROVED -> R.layout.item_booking_approved
            VIEW_TYPE_CONFIRMED -> R.layout.item_booking_confirmed
            VIEW_TYPE_COMPLETED -> R.layout.item_booking_completed
            VIEW_TYPE_CANCELLED -> R.layout.item_booking_cancelled
            else -> R.layout.item_booking_pending
        }
        
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = bookings[position]

        holder.txtBookingId.text = booking.bookingId
        holder.txtStatus.text = booking.status ?: "Unknown"
        holder.txtCustomer.text = "👤 Customer: ${booking.evOwnerNic ?: "N/A"}"
        holder.txtSlot.text = "⚡ Slot ${booking.slotNumber ?: "N/A"}"
        
        val reservationTime = formatDateTime(booking.reservationDateTime)
        holder.txtReservationTime.text = "📅 Scheduled: $reservationTime"
        
        holder.txtDuration.text = "⏱️ Duration: ${booking.duration ?: 0} hour(s)"
        holder.txtTimeUntil?.text = booking.timeUntilReservation ?: ""

        // Status-specific fields
        when (booking.status?.lowercase()) {
            "confirmed" -> {
                val confirmedTime = formatDateTime(booking.confirmedAt)
                holder.txtArrivalTime?.text = "🕒 Arrived: $confirmedTime"
                holder.btnComplete?.setOnClickListener { onCompleteClick(booking) }
            }
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
            "pending" -> {
                holder.btnApprove?.setOnClickListener { onApproveClick(booking) }
            }
        }

        // Set card colors based on status
        val strokeColor = when (booking.status?.lowercase()) {
            "pending" -> holder.itemView.context.getColor(R.color.warning)
            "approved" -> holder.itemView.context.getColor(R.color.info)
            "confirmed" -> holder.itemView.context.getColor(R.color.success)
            "completed" -> holder.itemView.context.getColor(R.color.primary)
            "cancelled" -> holder.itemView.context.getColor(R.color.error)
            else -> holder.itemView.context.getColor(R.color.divider)
        }
        holder.card.strokeColor = strokeColor
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<BookingResponseData>) {
        bookings = newBookings
        notifyDataSetChanged()
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

    companion object {
        private const val VIEW_TYPE_PENDING = 1
        private const val VIEW_TYPE_APPROVED = 2
        private const val VIEW_TYPE_CONFIRMED = 3
        private const val VIEW_TYPE_COMPLETED = 4
        private const val VIEW_TYPE_CANCELLED = 5
    }
}
