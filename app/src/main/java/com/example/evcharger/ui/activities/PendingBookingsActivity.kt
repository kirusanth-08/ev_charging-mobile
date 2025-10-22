package com.example.evcharger.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.evcharger.databinding.ActivityPendingBookingsBinding
import com.example.evcharger.model.BookingResponseData
import com.example.evcharger.model.Reservation
import com.example.evcharger.repository.ReservationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PendingBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPendingBookingsBinding
    private val adapter = ReservationAdapter()
    private val repo = ReservationRepository()
    private var loadJob: Job? = null
    private var stationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPendingBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get station ID from intent if provided (for operator to filter by station)
        stationId = intent.getStringExtra("stationId")

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadData() }

        loadData()
    }

    private fun loadData() {
        // Cancel any existing refresh job so we don't run multiple parallel requests
        loadJob?.cancel()
        binding.swipeRefresh.isRefreshing = true

        loadJob = lifecycleScope.launch {
            try {
                // If stationId is provided, fetch bookings for that specific station
                // Otherwise fetch all pending bookings (existing behavior)
                if (stationId != null) {
                    val res = withContext(Dispatchers.IO) { repo.getOperatorBookings(stationId!!) }
                    if (res.isSuccessful && res.body()?.success == true) {
                        val bookings = res.body()?.data ?: emptyList<BookingResponseData>()
                        // Convert BookingResponseData to Reservation for adapter compatibility
                        // Note: You may need to create a new adapter for BookingResponseData
                        // or convert the data to match the existing Reservation model
                        adapter.submitList(emptyList<Reservation>()) // Placeholder - needs proper conversion
                        Snackbar.make(binding.root, "Loaded ${bookings.size} bookings for station", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(binding.root, res.body()?.message ?: "Failed to load bookings", Snackbar.LENGTH_LONG).show()
                    }
                } else {
                    val res = withContext(Dispatchers.IO) { repo.getPending() }
                    if (res.isSuccessful) {
                        adapter.submitList(res.body()?.data ?: emptyList<Reservation>())
                    } else {
                        Snackbar.make(binding.root, res.body()?.message ?: "Failed to load pending", Snackbar.LENGTH_LONG).show()
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
}