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
        // Show booking details in a dialog
        val message = buildString {
            append("Booking ID: ${booking.bookingId}\n\n")
            append("Station: ${booking.stationName}\n")
            append("Location: ${booking.stationLocation}\n")
            append("Slot: ${booking.slotNumber}\n")
            append("Duration: ${booking.duration} hours\n")
            append("Status: ${booking.status}\n\n")
            
            if (booking.timeUntilReservation != null) {
                append("Time until reservation: ${booking.timeUntilReservation}\n\n")
            }
            
            if (booking.canCancel == true) {
                append("✓ You can cancel this booking\n")
            }
            if (booking.canModify == true) {
                append("✓ You can modify this booking")
            }
        }

        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📋 Booking Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
        
        // Add update button if canModify is true
        if (booking.canModify == true) {
            dialogBuilder.setNeutralButton("Update") { _, _ ->
                openUpdateBooking(booking)
            }
        }
        
        // Add cancel button if canCancel is true
        if (booking.canCancel == true) {
            dialogBuilder.setNegativeButton("Cancel Booking") { _, _ ->
                confirmCancelBooking(booking)
            }
        }
        
        dialogBuilder.show()
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
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Cancel Booking")
            .setMessage(
                "Are you sure you want to cancel this booking?\n\n" +
                "Booking ID: ${booking.bookingId}\n" +
                "Station: ${booking.stationName}\n" +
                "Slot: ${booking.slotNumber}\n\n" +
                "This action cannot be undone."
            )
            .setPositiveButton("Yes, Cancel") { _, _ ->
                cancelBooking(booking.bookingId)
            }
            .setNegativeButton("No", null)
            .show()
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