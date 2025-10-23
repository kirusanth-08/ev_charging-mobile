package com.example.evcharger.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.evcharger.databinding.ActivityPendingBookingsBinding
import com.example.evcharger.model.BookingResponseData
import com.example.evcharger.repository.ReservationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.evcharger.utils.StatusBarUtil
import com.example.evcharger.auth.UserSessionManager
import com.example.evcharger.network.RetrofitClient

/**
 * Activity to display pending bookings (bookings awaiting operator approval)
 */
class PendingBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPendingBookingsBinding
    private val adapter = BookingAdapter { booking ->
        // Handle booking click - show details or options
        showBookingDetails(booking)
    }
    private val repo = ReservationRepository()
    private var loadJob: Job? = null
    private var stationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPendingBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set green status bar to match header
        StatusBarUtil.setGreen(this)

        // Setup toolbar navigation
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }

        // Set up auth token
        setupAuthToken()

        // Get station ID from intent if provided (for operator to filter by station)
        stationId = intent.getStringExtra("stationId")

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadData() }

        loadData()
    }

    private fun setupAuthToken() {
        val sessionManager = UserSessionManager(this)
        val session = sessionManager.loadSession()
        session.token?.let { token ->
            RetrofitClient.setAuthToken(token)
        }
    }

    private fun loadData() {
        // Cancel any existing refresh job so we don't run multiple parallel requests
        loadJob?.cancel()
        binding.swipeRefresh.isRefreshing = true

        loadJob = lifecycleScope.launch {
            try {
                // If stationId is provided, fetch bookings for that specific station (operator view)
                // Otherwise fetch all pending bookings for the logged-in user
                if (stationId != null) {
                    val res = withContext(Dispatchers.IO) { repo.getOperatorBookings(stationId!!) }
                    if (res.isSuccessful && res.body()?.success == true) {
                        val bookings = res.body()?.data ?: emptyList()
                        adapter.submitList(bookings)
                        
                        if (bookings.isEmpty()) {
                            Snackbar.make(binding.root, "No pending bookings for this station", Snackbar.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorMsg = res.body()?.message ?: "Failed to load bookings"
                        Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG).show()
                    }
                } else {
                    val res = withContext(Dispatchers.IO) { repo.getPending() }
                    if (res.isSuccessful && res.body()?.success == true) {
                        val bookings = res.body()?.data ?: emptyList()
                        adapter.submitList(bookings)
                        
                        if (bookings.isEmpty()) {
                            Snackbar.make(binding.root, "No pending bookings", Snackbar.LENGTH_SHORT).show()
                        } else {
                            Snackbar.make(binding.root, "Loaded ${bookings.size} pending booking(s)", Snackbar.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorMsg = res.body()?.message ?: "Failed to load pending bookings"
                        Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (t: Throwable) {
                // Handle network / unexpected errors and ensure UI is updated
                Snackbar.make(binding.root, t.localizedMessage ?: "Error fetching pending bookings", Snackbar.LENGTH_LONG).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showBookingDetails(booking: BookingResponseData) {
        // Inflate custom dialog layout
        val dialogView = layoutInflater.inflate(com.example.evcharger.R.layout.dialog_booking_details, null)
        
        // Create dialog
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // Make dialog background transparent to show custom rounded corners
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Bind views
        val tvBookingId = dialogView.findViewById<android.widget.TextView>(com.example.evcharger.R.id.tvBookingId)
        val tvStationName = dialogView.findViewById<android.widget.TextView>(com.example.evcharger.R.id.tvStationName)
        val tvLocation = dialogView.findViewById<android.widget.TextView>(com.example.evcharger.R.id.tvLocation)
        val tvSlot = dialogView.findViewById<android.widget.TextView>(com.example.evcharger.R.id.tvSlot)
        val tvDateTime = dialogView.findViewById<android.widget.TextView>(com.example.evcharger.R.id.tvDateTime)
        val tvDuration = dialogView.findViewById<android.widget.TextView>(com.example.evcharger.R.id.tvDuration)
        val chipStatus = dialogView.findViewById<com.google.android.material.chip.Chip>(com.example.evcharger.R.id.chipStatus)
        val layoutTimeUntil = dialogView.findViewById<android.widget.LinearLayout>(com.example.evcharger.R.id.layoutTimeUntil)
        val tvTimeUntil = dialogView.findViewById<android.widget.TextView>(com.example.evcharger.R.id.tvTimeUntil)
        val cardPermissions = dialogView.findViewById<androidx.cardview.widget.CardView>(com.example.evcharger.R.id.cardPermissions)
        val layoutCanModify = dialogView.findViewById<android.widget.LinearLayout>(com.example.evcharger.R.id.layoutCanModify)
        val layoutCanCancel = dialogView.findViewById<android.widget.LinearLayout>(com.example.evcharger.R.id.layoutCanCancel)
        val btnClose = dialogView.findViewById<android.widget.ImageButton>(com.example.evcharger.R.id.btnClose)
        val btnUpdate = dialogView.findViewById<android.widget.Button>(com.example.evcharger.R.id.btnUpdate)
        val btnCancel = dialogView.findViewById<android.widget.Button>(com.example.evcharger.R.id.btnCancel)
        
        // Set data
        tvBookingId.text = booking.bookingId
        tvStationName.text = booking.stationName ?: "Unknown Station"
        tvLocation.text = booking.stationLocation ?: "Location not available"
        tvSlot.text = "Slot #${booking.slotNumber}"
        tvDateTime.text = booking.reservationDateTime ?: "Not specified"
        tvDuration.text = "${booking.duration} hours"
        chipStatus.text = booking.status?.uppercase() ?: "PENDING"
        
        // Set status chip color
        when (booking.status?.lowercase()) {
            "approved" -> chipStatus.setChipBackgroundColorResource(com.example.evcharger.R.color.success)
            "pending" -> chipStatus.setChipBackgroundColorResource(com.example.evcharger.R.color.warning)
            "cancelled" -> chipStatus.setChipBackgroundColorResource(com.example.evcharger.R.color.error)
            else -> chipStatus.setChipBackgroundColorResource(com.example.evcharger.R.color.text_secondary_light)
        }
        
        // Handle time until reservation
        if (booking.timeUntilReservation != null) {
            layoutTimeUntil.visibility = android.view.View.VISIBLE
            tvTimeUntil.text = "Time until reservation: ${booking.timeUntilReservation}"
        } else {
            layoutTimeUntil.visibility = android.view.View.GONE
        }
        
        // Handle permissions
        val hasPermissions = booking.canModify == true || booking.canCancel == true
        if (hasPermissions) {
            cardPermissions.visibility = android.view.View.VISIBLE
            layoutCanModify.visibility = if (booking.canModify == true) android.view.View.VISIBLE else android.view.View.GONE
            layoutCanCancel.visibility = if (booking.canCancel == true) android.view.View.VISIBLE else android.view.View.GONE
        } else {
            cardPermissions.visibility = android.view.View.GONE
        }
        
        // Handle action buttons
        if (booking.canModify == true) {
            btnUpdate.visibility = android.view.View.VISIBLE
            btnUpdate.setOnClickListener {
                dialog.dismiss()
                openUpdateBooking(booking)
            }
        } else {
            btnUpdate.visibility = android.view.View.GONE
        }
        
        if (booking.canCancel == true) {
            btnCancel.visibility = android.view.View.VISIBLE
            btnCancel.setOnClickListener {
                dialog.dismiss()
                confirmCancelBooking(booking)
            }
        } else {
            btnCancel.visibility = android.view.View.GONE
        }
        
        // Close button
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun openUpdateBooking(booking: BookingResponseData) {
        val intent = android.content.Intent(this, UpdateBookingActivity::class.java).apply {
            putExtra("bookingId", booking.bookingId)
            putExtra("evOwnerNic", booking.evOwnerNic)
            putExtra("stationId", booking.stationId)
            putExtra("stationName", booking.stationName)
            putExtra("stationLocation", booking.stationLocation)
            putExtra("slotNumber", booking.slotNumber)
            putExtra("reservationDateTime", booking.reservationDateTime)
            putExtra("duration", booking.duration)
            putExtra("status", booking.status)
        }
        startActivityForResult(intent, REQUEST_UPDATE_BOOKING)
    }
    
    private fun confirmCancelBooking(booking: BookingResponseData) {
        // Inflate custom cancel dialog layout
        val dialogView = layoutInflater.inflate(com.example.evcharger.R.layout.dialog_cancel_booking, null)
        
        // Create dialog
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // Make dialog background transparent
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Bind views
        val tvCancelBookingId = dialogView.findViewById<android.widget.TextView>(com.example.evcharger.R.id.tvCancelBookingId)
        val tvCancelStationName = dialogView.findViewById<android.widget.TextView>(com.example.evcharger.R.id.tvCancelStationName)
        val tvCancelSlot = dialogView.findViewById<android.widget.TextView>(com.example.evcharger.R.id.tvCancelSlot)
        val btnCancelNo = dialogView.findViewById<android.widget.Button>(com.example.evcharger.R.id.btnCancelNo)
        val btnCancelYes = dialogView.findViewById<android.widget.Button>(com.example.evcharger.R.id.btnCancelYes)
        
        // Set data
        tvCancelBookingId.text = booking.bookingId
        tvCancelStationName.text = booking.stationName ?: "Unknown"
        tvCancelSlot.text = "#${booking.slotNumber}"
        
        // Handle buttons
        btnCancelNo.setOnClickListener {
            dialog.dismiss()
        }
        
        btnCancelYes.setOnClickListener {
            dialog.dismiss()
            cancelBooking(booking.bookingId)
        }
        
        dialog.show()
    }
    
    private fun cancelBooking(bookingId: String) {
        binding.swipeRefresh.isRefreshing = true
        
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { 
                    repo.cancelBooking(bookingId) 
                }
                
                binding.swipeRefresh.isRefreshing = false
                
                if (result.isSuccess) {
                    Snackbar.make(
                        binding.root, 
                        "✅ Booking cancelled successfully", 
                        Snackbar.LENGTH_LONG
                    ).show()
                    
                    // Reload data
                    loadData()
                } else {
                    Snackbar.make(
                        binding.root, 
                        "Error: ${result.exceptionOrNull()?.message}", 
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Snackbar.make(
                    binding.root, 
                    "Error: ${t.localizedMessage}", 
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_UPDATE_BOOKING && resultCode == RESULT_OK) {
            // Reload data after successful update
            loadData()
        }
    }
    
    companion object {
        private const val REQUEST_UPDATE_BOOKING = 1001
    }
}