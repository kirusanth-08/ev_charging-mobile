package com.example.evcharger.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.evcharger.databinding.ActivityOperatorPendingBookingsBinding
import com.example.evcharger.model.BookingResponseData
import com.example.evcharger.network.RetrofitClient
import com.example.evcharger.repository.ReservationRepository
import com.example.evcharger.ui.adapters.OperatorBookingAdapter
import com.example.evcharger.utils.StatusBarUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for station operators to view pending charging requests
 * Uses GET /api/booking/pending endpoint to fetch all pending bookings
 * for the authenticated operator's stations
 */
class OperatorPendingBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperatorPendingBookingsBinding
    private lateinit var adapter: OperatorBookingAdapter
    private val pendingBookings = mutableListOf<BookingResponseData>()
    private val reservationRepository = ReservationRepository()
    private var stationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorPendingBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color
        StatusBarUtil.setGreen(this)

    setupToolbar()
    setupRecyclerView()

    // If activity was started with a specific stationId, use it to filter pending bookings
    stationId = intent.getStringExtra("stationId")

    loadPendingBookings()

        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadPendingBookings()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Pending Requests"
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = OperatorBookingAdapter(
            bookings = pendingBookings,
            onApproveClick = { booking -> handleApproveBooking(booking) },
            onRejectClick = { booking -> handleRejectBooking(booking) },
            onViewDetailsClick = { booking -> handleViewDetails(booking) }
        )

        binding.rvPendingBookings.apply {
            layoutManager = LinearLayoutManager(this@OperatorPendingBookingsActivity)
            adapter = this@OperatorPendingBookingsActivity.adapter
        }
    }

    private fun loadPendingBookings() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    // Use the new operator pending endpoint that returns OperatorPendingResponse
                    reservationRepository.getOperatorPendingBookings(stationId)
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.success == true) {
                        val operatorResponse = response.body()!!
                        val bookings = operatorResponse.bookings ?: emptyList()

                        // Filter only pending status bookings for operators (defensive)
                        val pending = bookings.filter {
                            it.status?.equals("Pending", ignoreCase = true) == true
                        }

                        pendingBookings.clear()
                        pendingBookings.addAll(pending)
                        adapter.notifyDataSetChanged()

                        updateEmptyState()
                        
                        // Show count and station info if available
                        val countText = if (stationId != null) {
                            "${pending.size} pending request(s) for Station ${operatorResponse.stationId ?: stationId}"
                        } else {
                            "${pending.size} pending request(s) across ${operatorResponse.stationId ?: "all stations"}"
                        }
                        binding.tvBookingCount.text = countText
                    } else {
                        val errorMsg = response.body()?.message ?: "Failed to load pending bookings"
                        showError(errorMsg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError(e.localizedMessage ?: "Network error occurred")
                }
            }
        }
    }

    private fun handleApproveBooking(booking: BookingResponseData) {
        // Show confirmation dialog
        MaterialAlertDialogBuilder(this)
            .setTitle("Approve Booking")
            .setMessage("Are you sure you want to approve booking ${booking.bookingId}?")
            .setPositiveButton("Approve") { _, _ ->
                approveBooking(booking)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun approveBooking(booking: BookingResponseData) {
        lifecycleScope.launch {
            try {
                showLoading(true)
                
                withContext(Dispatchers.IO) {
                    val result = reservationRepository.approveBooking(booking.bookingId)
                    
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        
                        result.fold(
                            onSuccess = { approvedBooking ->
                                Toast.makeText(
                                    this@OperatorPendingBookingsActivity,
                                    "Booking ${booking.bookingId} approved successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                // Remove from the list and refresh UI
                                pendingBookings.remove(booking)
                                adapter.notifyDataSetChanged()
                                binding.tvBookingCount.text = "${pendingBookings.size} pending request(s)"
                                updateEmptyState()
                            },
                            onFailure = { error ->
                                showError(error.localizedMessage ?: "Failed to approve booking")
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError(e.localizedMessage ?: "Network error occurred")
                }
            }
        }
    }

    private fun handleRejectBooking(booking: BookingResponseData) {
        // Show confirmation dialog
        MaterialAlertDialogBuilder(this)
            .setTitle("Reject Booking")
            .setMessage("Are you sure you want to reject booking ${booking.bookingId}? This action cannot be undone.")
            .setPositiveButton("Reject") { _, _ ->
                rejectBooking(booking)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectBooking(booking: BookingResponseData) {
        lifecycleScope.launch {
            try {
                showLoading(true)
                
                withContext(Dispatchers.IO) {
                    val result = reservationRepository.cancelBooking(booking.bookingId)
                    
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        
                        result.fold(
                            onSuccess = {
                                Toast.makeText(
                                    this@OperatorPendingBookingsActivity,
                                    "Booking ${booking.bookingId} rejected successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                // Remove from the list and refresh UI
                                pendingBookings.remove(booking)
                                adapter.notifyDataSetChanged()
                                binding.tvBookingCount.text = "${pendingBookings.size} pending request(s)"
                                updateEmptyState()
                            },
                            onFailure = { error ->
                                showError(error.localizedMessage ?: "Failed to reject booking")
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError(e.localizedMessage ?: "Network error occurred")
                }
            }
        }
    }

    private fun handleViewDetails(booking: BookingResponseData) {
        val details = buildString {
            appendLine("Booking ID: ${booking.bookingId}")
            appendLine("Customer NIC: ${booking.evOwnerNic ?: "N/A"}")
            appendLine("Station: ${booking.stationName ?: "N/A"}")
            appendLine("Location: ${booking.stationLocation ?: "N/A"}")
            appendLine("Slot Number: ${booking.slotNumber ?: "N/A"}")
            appendLine("Reservation Time: ${booking.reservationDateTime ?: "N/A"}")
            appendLine("Duration: ${booking.duration ?: 0} hour(s)")
            appendLine("Status: ${booking.status ?: "N/A"}")
            appendLine("Created At: ${booking.createdAt ?: "N/A"}")
            if (booking.approvedBy != null) {
                appendLine("Approved By: ${booking.approvedBy}")
            }
            if (booking.approvedAt != null) {
                appendLine("Approved At: ${booking.approvedAt}")
            }
            if (booking.energyConsumed != null) {
                appendLine("Energy Consumed: ${booking.energyConsumed} kWh")
            }
            if (booking.cost != null) {
                appendLine("Cost: LKR ${booking.cost}")
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Booking Details")
            .setMessage(details)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.swipeRefresh.isRefreshing = show
        binding.progressBar.visibility = if (show && !binding.swipeRefresh.isRefreshing) View.VISIBLE else View.GONE
    }

    private fun updateEmptyState() {
        if (pendingBookings.isEmpty()) {
            binding.rvPendingBookings.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.rvPendingBookings.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        updateEmptyState()
    }
}
