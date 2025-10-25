package com.example.evcharger.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.evcharger.R
import com.example.evcharger.auth.UserSessionManager
import com.example.evcharger.databinding.ActivityOperatorConfirmedArrivalsBinding
import com.example.evcharger.model.BookingResponseData
import com.example.evcharger.network.RetrofitClient
import com.example.evcharger.ui.adapters.ConfirmedArrivalAdapter
import com.example.evcharger.utils.StatusBarUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OperatorConfirmedArrivalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperatorConfirmedArrivalsBinding
    private lateinit var sessionManager: UserSessionManager
    private lateinit var adapter: ConfirmedArrivalAdapter
    private var confirmedBookings: MutableList<BookingResponseData> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorConfirmedArrivalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar
        StatusBarUtil.setGreen(this)

        sessionManager = UserSessionManager(this)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Confirmed Arrivals"

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Setup RecyclerView
        setupRecyclerView()

        // Load confirmed arrivals
        loadConfirmedArrivals()

        // Setup refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadConfirmedArrivals()
        }
    }

    private fun setupRecyclerView() {
        adapter = ConfirmedArrivalAdapter(
            bookings = confirmedBookings,
            onCompleteClick = { booking ->
                showCompleteBookingDialog(booking)
            }
        )

        binding.rvConfirmedArrivals.layoutManager = LinearLayoutManager(this)
        binding.rvConfirmedArrivals.adapter = adapter
    }

    private fun loadConfirmedArrivals() {
        binding.swipeRefresh.isRefreshing = true
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get all operator bookings
                val response = RetrofitClient.api.getOperatorBookings("")
                
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE

                    if (response.isSuccessful) {
                        val allBookings = response.body()?.data ?: emptyList()
                        
                        // Filter only "Confirmed" status bookings
                        val confirmedList = allBookings.filter { 
                            it.status?.equals("Confirmed", ignoreCase = true) == true 
                        }
                        
                        confirmedBookings.clear()
                        confirmedBookings.addAll(confirmedList)
                        adapter.notifyDataSetChanged()

                        // Show empty state if no confirmed arrivals
                        if (confirmedBookings.isEmpty()) {
                            binding.emptyState.visibility = View.VISIBLE
                            binding.txtEmptyMessage.text = "No confirmed arrivals at this time"
                        } else {
                            binding.emptyState.visibility = View.GONE
                        }

                        binding.txtConfirmedCount.text = "${confirmedBookings.size} confirmed arrival(s)"
                    } else {
                        val errorMsg = response.body()?.message ?: "Failed to load bookings"
                        Toast.makeText(this@OperatorConfirmedArrivalsActivity, errorMsg, Toast.LENGTH_SHORT).show()
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
                        this@OperatorConfirmedArrivalsActivity,
                        ex.localizedMessage ?: "Failed to load bookings",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showCompleteBookingDialog(booking: BookingResponseData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_complete_booking, null)
        val etEnergyConsumed = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEnergyConsumed)
        val etCost = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCost)

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
                            this@OperatorConfirmedArrivalsActivity,
                            "Booking completed successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Reload the list
                        loadConfirmedArrivals()
                    } else {
                        val errorMsg = response.body()?.message ?: "Failed to complete booking"
                        Toast.makeText(this@OperatorConfirmedArrivalsActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@OperatorConfirmedArrivalsActivity,
                        ex.localizedMessage ?: "Failed to complete booking",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
