package com.example.evcharger.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.evcharger.databinding.ActivityBookingListBinding
import com.example.evcharger.model.Reservation
import com.example.evcharger.repository.ReservationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shows upcoming and past bookings in a list (RecyclerView).
 */
class BookingListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingListBinding
    private lateinit var nic: String
    private val adapter = ReservationAdapter()
    private val repo = ReservationRepository()
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nic = intent.getStringExtra("NIC") ?: ""

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadData() }

        // Back button - navigate back to previous screen
        binding.btnBack?.setOnClickListener {
            finish()
        }

        loadData()
    }

    private fun loadData() {
        loadJob?.cancel()
        binding.swipeRefresh.isRefreshing = true

        loadJob = lifecycleScope.launch {
            try {
                val upcoming = withContext(Dispatchers.IO) { repo.getUpcoming(nic) }
                val history = withContext(Dispatchers.IO) { repo.getHistory(nic) }
                if (upcoming.isSuccessful && history.isSuccessful) {
                    val list = mutableListOf<Reservation>()
                    upcoming.body()?.data?.let { list.addAll(it) }
                    history.body()?.data?.let { list.addAll(it) }
                    adapter.submitList(list)
                } else {
                    Snackbar.make(binding.root, "Failed to load bookings", Snackbar.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                Snackbar.make(binding.root, t.localizedMessage ?: "Error loading bookings", Snackbar.LENGTH_LONG).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}