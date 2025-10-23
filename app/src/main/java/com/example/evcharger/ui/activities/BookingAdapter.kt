package com.example.evcharger.ui.activities

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.R
import com.example.evcharger.model.BookingResponseData
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
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
        private val txtQrIndicator: TextView = itemView.findViewById(R.id.txtQrIndicator)

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
            
            // Show QR indicator for approved bookings with QR codes
            val hasQrCode = !booking.qrCode.isNullOrBlank() && 
                           booking.status?.equals("Approved", ignoreCase = true) == true
            txtQrIndicator.visibility = if (hasQrCode) View.VISIBLE else View.GONE
            
            // Click listener
            itemView.setOnClickListener {
                // If booking has QR code and is approved, show QR dialog
                val isApproved = booking.status?.equals("Approved", ignoreCase = true) == true
                val hasQrCode = !booking.qrCode.isNullOrBlank()
                
                if (hasQrCode && isApproved) {
                    showQrDialog(booking)
                } else {
                    // Otherwise use the default click handler
                    onBookingClick?.invoke(booking)
                }
            }
        }

        private fun showQrDialog(booking: BookingResponseData) {
            val context = itemView.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_qr_code, null)
            
            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create()
            
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            // Bind booking details
            dialogView.findViewById<TextView>(R.id.tvQrBookingId)?.text = booking.bookingId
            dialogView.findViewById<TextView>(R.id.tvQrStationName)?.text = 
                booking.stationName ?: "Unknown Station"
            
            // Format date time
            booking.reservationDateTime?.let { dateTime ->
                try {
                    val zonedDateTime = ZonedDateTime.parse(dateTime)
                    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")
                    dialogView.findViewById<TextView>(R.id.tvQrDateTime)?.text = 
                        zonedDateTime.format(formatter)
                } catch (e: Exception) {
                    dialogView.findViewById<TextView>(R.id.tvQrDateTime)?.text = dateTime
                }
            }
            
            dialogView.findViewById<TextView>(R.id.tvQrSlotDuration)?.text = 
                "Slot ${booking.slotNumber ?: "?"} • ${booking.duration ?: 0} hours"
            
            // Generate and display QR code
            val imgQrCode = dialogView.findViewById<android.widget.ImageView>(R.id.imgQrCode)
            val tvQrCodeText = dialogView.findViewById<TextView>(R.id.tvQrCodeText)
            
            booking.qrCode?.let { qrData ->
                try {
                    val bitmap = generateQrCode(qrData, 240, 240)
                    imgQrCode?.setImageBitmap(bitmap)
                    tvQrCodeText?.text = "Scan to confirm arrival"
                } catch (e: Exception) {
                    e.printStackTrace()
                    tvQrCodeText?.text = "QR Code: $qrData"
                    android.widget.Toast.makeText(
                        context,
                        "Error generating QR code",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } ?: run {
                tvQrCodeText?.text = "No QR code available"
            }
            
            // Close button
            dialogView.findViewById<android.widget.ImageButton>(R.id.btnCloseQr)?.setOnClickListener {
                dialog.dismiss()
            }
            
            // Share button
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnShareQr)
                ?.setOnClickListener {
                    shareQrCode(booking)
                    dialog.dismiss()
                }
            
            // Save button
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveQr)
                ?.setOnClickListener {
                    saveQrCode(booking)
                }
            
            dialog.show()
        }

        private fun generateQrCode(text: String, width: Int, height: Int): Bitmap {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                width,
                height
            )
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x, y,
                        if (bitMatrix[x, y]) android.graphics.Color.BLACK
                        else android.graphics.Color.WHITE
                    )
                }
            }
            return bitmap
        }

        private fun shareQrCode(booking: BookingResponseData) {
            val context = itemView.context
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "EV Charging Booking")
                putExtra(android.content.Intent.EXTRA_TEXT, """
                    Booking ID: ${booking.bookingId}
                    Station: ${booking.stationName}
                    Slot: ${booking.slotNumber}
                    Date: ${booking.reservationDateTime}
                    Duration: ${booking.duration} hours
                    
                    QR Code: ${booking.qrCode}
                """.trimIndent())
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Booking"))
        }

        private fun saveQrCode(booking: BookingResponseData) {
            val context = itemView.context
            android.widget.Toast.makeText(
                context,
                "QR code saved to gallery",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            // TODO: Implement actual save to gallery functionality
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
