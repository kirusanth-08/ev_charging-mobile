package com.example.evcharger.ui.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.databinding.ItemInProgressBookingBinding
import com.example.evcharger.model.BookingResponseData
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class InProgressBookingAdapter(
    private val onCompleteClick: (BookingResponseData) -> Unit
) : ListAdapter<BookingResponseData, InProgressBookingAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BookingResponseData>() {
            override fun areItemsTheSame(
                oldItem: BookingResponseData,
                newItem: BookingResponseData
            ): Boolean = oldItem.bookingId == newItem.bookingId

            override fun areContentsTheSame(
                oldItem: BookingResponseData,
                newItem: BookingResponseData
            ): Boolean = oldItem == newItem
        }
    }

    inner class ViewHolder(
        private val binding: ItemInProgressBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: BookingResponseData) {
            binding.tvBookingId.text = booking.bookingId ?: "N/A"
            binding.tvStatus.text = booking.status?.uppercase() ?: "IN PROGRESS"
            
            binding.tvCustomerInfo.text = "Customer: ${booking.evOwnerNic ?: "N/A"}"
            
            // Format start time
            val startTime = try {
                val zonedDateTime = ZonedDateTime.parse(booking.reservationDateTime)
                val formatter = DateTimeFormatter.ofPattern("hh:mm a")
                zonedDateTime.format(formatter)
            } catch (e: Exception) {
                booking.reservationDateTime ?: "N/A"
            }
            
            binding.tvSlotInfo.text = 
                "Slot ${booking.slotNumber ?: "?"} • ${booking.duration ?: 0} hours • Started: $startTime"
            
            binding.btnComplete.setOnClickListener {
                onCompleteClick(booking)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInProgressBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
