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

class ConfirmedArrivalAdapter(
    private val bookings: List<BookingResponseData>,
    private val onCompleteClick: (BookingResponseData) -> Unit
) : RecyclerView.Adapter<ConfirmedArrivalAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardBooking)
        val txtBookingId: TextView = view.findViewById(R.id.txtBookingId)
        val txtStationInfo: TextView = view.findViewById(R.id.txtStationInfo)
        val txtSlotInfo: TextView = view.findViewById(R.id.txtSlotInfo)
        val txtReservationTime: TextView = view.findViewById(R.id.txtReservationTime)
        val txtDuration: TextView = view.findViewById(R.id.txtDuration)
        val txtCustomerInfo: TextView = view.findViewById(R.id.txtCustomerInfo)
        val txtConfirmedAt: TextView = view.findViewById(R.id.txtConfirmedAt)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        val btnComplete: MaterialButton = view.findViewById(R.id.btnComplete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_confirmed_arrival, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = bookings[position]

        holder.txtBookingId.text = "Booking: ${booking.bookingId}"
        holder.txtStationInfo.text = "${booking.stationName ?: "Unknown Station"}"
        holder.txtSlotInfo.text = "Slot ${booking.slotNumber ?: "N/A"}"
        
        // Format reservation time
        val reservationTime = formatDateTime(booking.reservationDateTime)
        holder.txtReservationTime.text = "Scheduled: $reservationTime"
        
        holder.txtDuration.text = "Duration: ${booking.duration ?: 0} hour(s)"
        holder.txtCustomerInfo.text = "Customer: ${booking.evOwnerNic ?: "N/A"}"
        
        // Format confirmed at time
        val confirmedTime = formatDateTime(booking.confirmedAt)
        holder.txtConfirmedAt.text = "Arrived at: $confirmedTime"
        
        // Status with color
        holder.txtStatus.text = "✓ ${booking.status ?: "Confirmed"}"
        holder.txtStatus.setTextColor(holder.itemView.context.getColor(R.color.success))

        // Complete button click
        holder.btnComplete.setOnClickListener {
            onCompleteClick(booking)
        }
    }

    override fun getItemCount(): Int = bookings.size

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
}
