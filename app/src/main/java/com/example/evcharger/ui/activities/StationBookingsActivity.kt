package com.example.evcharger.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.evcharger.databinding.ActivityStationBookingsBinding
import com.example.evcharger.model.BookingResponseData
import com.example.evcharger.network.RetrofitClient
import com.example.evcharger.ui.adapters.StationBookingsAdapter
import com.example.evcharger.utils.StatusBarUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StationBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStationBookingsBinding
    private lateinit var adapter: StationBookingsAdapter
    private var allBookings: MutableList<BookingResponseData> = mutableListOf()
    private var stationId: String = ""
    private var stationName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStationBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar
        StatusBarUtil.setGreen(this)

        // Get station details from intent
        stationId = intent.getStringExtra("stationId") ?: ""
        stationName = intent.getStringExtra("stationName") ?: "Station"

        if (stationId.isEmpty()) {
            Toast.makeText(this, "Station ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = stationName

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Setup RecyclerView
        setupRecyclerView()

        // Load bookings
        loadStationBookings()

        // Setup refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadStationBookings()
        }

        // Setup filter chips
        setupFilterChips()
    }

    private fun setupRecyclerView() {
        adapter = StationBookingsAdapter(
            bookings = allBookings,
            onCompleteClick = { booking ->
                showCompleteBookingDialog(booking)
            },
            onApproveClick = { booking ->
                approveBooking(booking)
            }
        )

        binding.rvBookings.layoutManager = LinearLayoutManager(this)
        binding.rvBookings.adapter = adapter
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterBookings("All")
        }
        binding.chipPending.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterBookings("Pending")
        }
        binding.chipApproved.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterBookings("Approved")
        }
        binding.chipConfirmed.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterBookings("Confirmed")
        }
        binding.chipCompleted.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterBookings("Completed")
        }
        binding.chipCancelled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterBookings("Cancelled")
        }
    }

    private fun loadStationBookings() {
        binding.swipeRefresh.isRefreshing = true
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.getOperatorBookings(stationId)

                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE

                    if (response.isSuccessful) {
                        val bookings = response.body()?.data ?: emptyList()

                        allBookings.clear()
                        allBookings.addAll(bookings)
                        
                        updateBookingCounts(bookings)
                        
                        // Apply current filter
                        val selectedChipId = binding.chipGroup.checkedChipId
                        when (selectedChipId) {
                            binding.chipPending.id -> filterBookings("Pending")
                            binding.chipApproved.id -> filterBookings("Approved")
                            binding.chipConfirmed.id -> filterBookings("Confirmed")
                            binding.chipCompleted.id -> filterBookings("Completed")
                            binding.chipCancelled.id -> filterBookings("Cancelled")
                            else -> filterBookings("All")
                        }

                        if (bookings.isEmpty()) {
                            binding.emptyState.visibility = View.VISIBLE
                            binding.txtEmptyMessage.text = "No bookings found for this station"
                        }
                    } else {
                        val errorMsg = response.body()?.message ?: "Failed to load bookings"
                        Toast.makeText(this@StationBookingsActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        binding.emptyState.visibility = View.VISIBLE
                        binding.txtEmptyMessage.text = errorMsg
                    }
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE
                    binding.txtEmptyMessage.text = "Error: ${ex.localizedMessage}"
                    Toast.makeText(
                        this@StationBookingsActivity,
                        ex.localizedMessage ?: "Failed to load bookings",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun updateBookingCounts(bookings: List<BookingResponseData>) {
        val pending = bookings.count { it.status?.equals("Pending", ignoreCase = true) == true }
        val approved = bookings.count { it.status?.equals("Approved", ignoreCase = true) == true }
        val confirmed = bookings.count { it.status?.equals("Confirmed", ignoreCase = true) == true }
        val completed = bookings.count { it.status?.equals("Completed", ignoreCase = true) == true }
        val cancelled = bookings.count { it.status?.equals("Cancelled", ignoreCase = true) == true }

        binding.chipAll.text = "All (${bookings.size})"
        binding.chipPending.text = "Pending ($pending)"
        binding.chipApproved.text = "Approved ($approved)"
        binding.chipConfirmed.text = "Confirmed ($confirmed)"
        binding.chipCompleted.text = "Completed ($completed)"
        binding.chipCancelled.text = "Cancelled ($cancelled)"
    }

    private fun filterBookings(status: String) {
        val filtered = if (status == "All") {
            allBookings
        } else {
            allBookings.filter { it.status?.equals(status, ignoreCase = true) == true }
        }

        adapter.updateBookings(filtered)

        if (filtered.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.txtEmptyMessage.text = "No $status bookings"
        } else {
            binding.emptyState.visibility = View.GONE
        }
    }

    private fun showCompleteBookingDialog(booking: BookingResponseData) {
        val dialogView = layoutInflater.inflate(com.example.evcharger.R.layout.dialog_complete_booking, null)
        val etEnergyConsumed = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.evcharger.R.id.etEnergyConsumed)
        val etCost = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.evcharger.R.id.etCost)

        MaterialAlertDialogBuilder(this)
            .setTitle("Complete Charging")
            .setMessage("Complete charging for ${booking.stationName} - Slot ${booking.slotNumber}")
            .setView(dialogView)
            .setPositiveButton("Complete") { _, _ ->
                val energyStr = etEnergyConsumed.text.toString()
                val costStr = etCost.text.toString()

                if (energyStr.isBlank() || costStr.isBlank()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val energy = energyStr.toDoubleOrNull()
                val cost = costStr.toDoubleOrNull()

                if (energy == null || cost == null) {
                    Toast.makeText(this, "Invalid energy or cost value", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                completeBooking(booking.bookingId, energy, cost)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun completeBooking(bookingId: String, energyConsumed: Double, cost: Double) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = com.example.evcharger.model.CompleteBookingRequest(
                    energyConsumed = energyConsumed,
                    cost = cost
                )

                val response = RetrofitClient.api.completeBooking(bookingId, request)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@StationBookingsActivity,
                            "Booking completed successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Reload the list
                        loadStationBookings()
                    } else {
                        val errorMsg = response.body()?.message ?: "Failed to complete booking"
                        Toast.makeText(this@StationBookingsActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@StationBookingsActivity,
                        ex.localizedMessage ?: "Failed to complete booking",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun approveBooking(booking: BookingResponseData) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.approveBooking(booking.bookingId)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@StationBookingsActivity,
                            "Booking approved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Reload the list
                        loadStationBookings()
                    } else {
                        val errorMsg = response.body()?.message ?: "Failed to approve booking"
                        Toast.makeText(this@StationBookingsActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@StationBookingsActivity,
                        ex.localizedMessage ?: "Failed to approve booking",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
