package com.example.evcharger.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.R
import com.example.evcharger.model.BookingResponseData
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Adapter for displaying pending booking requests for station operators
 */
class OperatorBookingAdapter(
    private val bookings: List<BookingResponseData>,
    private val onApproveClick: (BookingResponseData) -> Unit,
    private val onRejectClick: (BookingResponseData) -> Unit,
    private val onViewDetailsClick: (BookingResponseData) -> Unit
) : RecyclerView.Adapter<OperatorBookingAdapter.BookingViewHolder>() {

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBookingId: TextView = itemView.findViewById(R.id.tvBookingId)
        val tvCustomerNic: TextView = itemView.findViewById(R.id.tvCustomerNic)
        val tvStationName: TextView = itemView.findViewById(R.id.tvStationName)
        val tvSlotInfo: TextView = itemView.findViewById(R.id.tvSlotInfo)
        val tvReservationTime: TextView = itemView.findViewById(R.id.tvReservationTime)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnApprove: Button = itemView.findViewById(R.id.btnApprove)
        val btnReject: Button = itemView.findViewById(R.id.btnReject)
        val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_operator_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]

        holder.tvBookingId.text = "Booking: ${booking.bookingId}"
        holder.tvCustomerNic.text = "Customer: ${booking.evOwnerNic ?: "N/A"}"
        holder.tvStationName.text = booking.stationName ?: "Unknown Station"
        holder.tvSlotInfo.text = "Slot #${booking.slotNumber ?: "N/A"}"
        
        // Format reservation time
        booking.reservationDateTime?.let { isoString ->
            try {
                val dateTime = LocalDateTime.parse(isoString, DateTimeFormatter.ISO_DATE_TIME)
                val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")
                holder.tvReservationTime.text = dateTime.format(formatter)
            } catch (e: Exception) {
                holder.tvReservationTime.text = isoString
            }
        } ?: run {
            holder.tvReservationTime.text = "Time not specified"
        }

        holder.tvDuration.text = "Duration: ${booking.duration ?: 0} hours"
        holder.tvStatus.text = booking.status ?: "Unknown"

        // Set status color
        when (booking.status?.lowercase()) {
            "pending" -> {
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
            }
            "approved" -> {
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
            }
            "rejected", "cancelled" -> {
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            }
        }

        // Button click listeners
        holder.btnApprove.setOnClickListener { onApproveClick(booking) }
        holder.btnReject.setOnClickListener { onRejectClick(booking) }
        holder.btnViewDetails.setOnClickListener { onViewDetailsClick(booking) }
    }

    override fun getItemCount() = bookings.size
}
