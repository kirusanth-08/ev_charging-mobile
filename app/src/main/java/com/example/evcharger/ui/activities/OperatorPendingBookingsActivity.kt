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
import com.example.evcharger.ui.adapters.OperatorBookingAdapter
import com.example.evcharger.utils.StatusBarUtil
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorPendingBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color
        StatusBarUtil.setGreen(this)

        setupToolbar()
        setupRecyclerView()
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

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.getPending()

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.success == true) {
                        val bookings = response.body()?.data ?: emptyList()
                        
                        // Filter only pending status bookings for operators
                        val pending = bookings.filter { 
                            it.status?.equals("Pending", ignoreCase = true) == true 
                        }

                        pendingBookings.clear()
                        pendingBookings.addAll(pending)
                        adapter.notifyDataSetChanged()

                        updateEmptyState()
                        binding.tvBookingCount.text = "${pending.size} pending request(s)"
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
        // TODO: Implement approval logic
        // This might call a different endpoint like PATCH /api/booking/{id}/approve
        Toast.makeText(this, "Approve: ${booking.bookingId}", Toast.LENGTH_SHORT).show()
    }

    private fun handleRejectBooking(booking: BookingResponseData) {
        // TODO: Implement rejection logic
        // This might call DELETE /api/booking/{id} or PATCH with rejection status
        Toast.makeText(this, "Reject: ${booking.bookingId}", Toast.LENGTH_SHORT).show()
    }

    private fun handleViewDetails(booking: BookingResponseData) {
        // TODO: Navigate to booking details screen
        Toast.makeText(this, "View details: ${booking.bookingId}", Toast.LENGTH_SHORT).show()
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
