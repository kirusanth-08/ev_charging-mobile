package com.example.evcharger.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.evcharger.R
import com.example.evcharger.auth.UserSessionManager
import com.example.evcharger.databinding.ActivityBookingHistoryBinding
import com.example.evcharger.network.RetrofitClient
import com.example.evcharger.repository.ReservationRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity to display booking history (completed, cancelled bookings)
 */
class BookingHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingHistoryBinding
    private val adapter = BookingHistoryAdapter { booking ->
        // Handle booking click - navigate to station details
        booking.stationId?.let { stationId ->
            val intent = Intent(this, StationDetailsActivity::class.java)
            intent.putExtra("stationId", stationId)
            startActivity(intent)
        } ?: run {
            Snackbar.make(binding.root, "Station information not available", Snackbar.LENGTH_SHORT).show()
        }
    }
    private lateinit var repo: ReservationRepository
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize repository with context for caching
        repo = ReservationRepository(this)

        // Setup toolbar
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }

        // Set up auth token
        setupAuthToken()

        // Setup RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Setup swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadHistory()
        }

        // Load initial data
        loadHistory()
    }

    private fun setupAuthToken() {
        val sessionManager = UserSessionManager(this)
        val session = sessionManager.loadSession()
        session.token?.let { token ->
            RetrofitClient.setAuthToken(token)
        }
    }

    private fun loadHistory() {
        // Cancel any existing job
        loadJob?.cancel()
        binding.swipeRefresh.isRefreshing = true

        loadJob = lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { repo.getHistory() }

                if (res.isSuccessful && res.body()?.success == true) {
                    val bookings = res.body()?.data ?: emptyList()
                    
                    if (bookings.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                        adapter.submitList(bookings)
                    }
                } else {
                    val errorMsg = res.body()?.message ?: getString(R.string.error_loading_history)
                    Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG).show()
                    showEmptyState()
                }
            } catch (t: Throwable) {
                Snackbar.make(
                    binding.root,
                    t.localizedMessage ?: getString(R.string.error_loading_history),
                    Snackbar.LENGTH_LONG
                ).show()
                showEmptyState()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showEmptyState() {
        binding.emptyState.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.emptyState.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
    }
}
