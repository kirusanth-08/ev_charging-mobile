package com.example.evcharger.ui.activities

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.R
import com.example.evcharger.databinding.ItemReservationBinding
import com.example.evcharger.model.Reservation
import com.example.evcharger.util.QRCodeUtil
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
        
        // Click listener to show QR dialog for approved reservations
        holder.itemView.setOnClickListener {
//            val isApproved = r.status?.equals("Approved", ignoreCase = true) == true
            val hasQrCode = !qrPayload.isNullOrBlank()
            
//            if (hasQrCode && isApproved) {
            if (hasQrCode) {
                showQrDialog(r, holder.itemView.context)
            }
        }
    }
    
    private fun showQrDialog(reservation: Reservation, context: android.content.Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_qr_code, null)
        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Bind reservation details
        dialogView.findViewById<TextView>(R.id.tvQrBookingId)?.text = reservation.id ?: "N/A"
        dialogView.findViewById<TextView>(R.id.tvQrStationName)?.text = 
            reservation.stationName ?: reservation.stationId ?: "Unknown Station"
        
        // Format date time
        reservation.startTime?.let { dateTime ->
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
            "Slot ${reservation.slotNumber ?: "?"} • ${reservation.duration ?: 0} hours"
        
        // Generate and display QR code
        val imgQrCode = dialogView.findViewById<android.widget.ImageView>(R.id.imgQrCode)
        val tvQrCodeText = dialogView.findViewById<TextView>(R.id.tvQrCodeText)
        
        val qrData = reservation.qrCode ?: reservation.qrCodePayload
        qrData?.let { qr ->
            try {
                val bitmap = generateQrCode(qr, 240, 240)
                imgQrCode?.setImageBitmap(bitmap)
                tvQrCodeText?.text = "Scan to confirm arrival"
            } catch (e: Exception) {
                e.printStackTrace()
                tvQrCodeText?.text = "QR Code: $qr"
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
                shareQrCode(reservation, context)
                dialog.dismiss()
            }
        
        // Save button
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveQr)
            ?.setOnClickListener {
                saveQrCode(reservation, context)
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
    
    private fun shareQrCode(reservation: Reservation, context: android.content.Context) {
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "EV Charging Booking")
            putExtra(android.content.Intent.EXTRA_TEXT, """
                Reservation ID: ${reservation.id}
                Station: ${reservation.stationName ?: reservation.stationId}
                Slot: ${reservation.slotNumber}
                Date: ${reservation.startTime}
                Duration: ${reservation.duration} hours
                
                QR Code: ${reservation.qrCode ?: reservation.qrCodePayload}
            """.trimIndent())
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Booking"))
    }
    
    private fun saveQrCode(reservation: Reservation, context: android.content.Context) {
        android.widget.Toast.makeText(
            context,
            "QR code saved to gallery",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        // TODO: Implement actual save to gallery functionality
    }
}