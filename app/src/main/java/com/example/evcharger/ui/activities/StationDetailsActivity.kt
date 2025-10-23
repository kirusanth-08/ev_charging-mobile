package com.example.evcharger.ui.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.evcharger.R
import com.example.evcharger.auth.UserSessionManager
import com.example.evcharger.databinding.ActivityStationDetailsBinding
import com.example.evcharger.network.RetrofitClient
import com.example.evcharger.repository.ReservationRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity to display detailed information about a charging station
 */
class StationDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStationDetailsBinding
    private lateinit var repo: ReservationRepository
    private lateinit var stationId: String
    private val slotsAdapter = StationSlotsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStationDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize repository with context for caching
        repo = ReservationRepository(this)

        // Get station ID from intent
        stationId = intent.getStringExtra("stationId") ?: run {
            Snackbar.make(binding.root, "Station ID not provided", Snackbar.LENGTH_LONG).show()
            finish()
            return
        }

        // Setup toolbar
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }

        // Set up auth token
        setupAuthToken()

        // Setup slots RecyclerView
        binding.recyclerSlots.layoutManager = LinearLayoutManager(this)
        binding.recyclerSlots.adapter = slotsAdapter

        // Load station details
        loadStationDetails()
    }

    private fun setupAuthToken() {
        val sessionManager = UserSessionManager(this)
        val session = sessionManager.loadSession()
        session.token?.let { token ->
            RetrofitClient.setAuthToken(token)
        }
    }

    private fun loadStationDetails() {
        binding.progressBar.visibility = View.VISIBLE
        binding.cardStationInfo.visibility = View.GONE
        binding.cardSlotsInfo.visibility = View.GONE
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { repo.getStationDetails(stationId) }

                if (res.isSuccessful && res.body()?.success == true) {
                    val station = res.body()?.data
                    val isFromCache = res.body()?.message?.contains("offline cache", ignoreCase = true) == true
                    
                    if (station != null) {
                        // Show offline indicator if data is from cache
                        if (isFromCache) {
                            Snackbar.make(
                                binding.root,
                                "📡 Offline Mode: Showing cached station data",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        
                        // Display station info
                        binding.tvStationName.text = station.name
                        binding.tvStationId.text = station.stationId
                        binding.tvLocation.text = "${station.location.address ?: ""}, ${station.location.city ?: ""}".trim(',', ' ')
                        
                        // Station type
                        if (!station.type.isNullOrEmpty()) {
                            binding.layoutStationType.visibility = View.VISIBLE
                            binding.tvStationType.text = station.type
                        } else {
                            binding.layoutStationType.visibility = View.GONE
                        }
                        
                        // Active status
                        if (station.isActive) {
                            binding.tvActiveStatus.text = getString(R.string.station_active)
                            binding.tvActiveStatus.setTextColor(ContextCompat.getColor(this@StationDetailsActivity, R.color.success))
                        } else {
                            binding.tvActiveStatus.text = getString(R.string.station_inactive)
                            binding.tvActiveStatus.setTextColor(ContextCompat.getColor(this@StationDetailsActivity, R.color.error))
                        }
                        
                        // Total slots
                        if (station.totalSlots != null) {
                            binding.layoutTotalSlots.visibility = View.VISIBLE
                            binding.tvTotalSlots.text = station.totalSlots.toString()
                        } else {
                            binding.layoutTotalSlots.visibility = View.GONE
                        }
                        
                        // Available slots
                        if (station.availableSlots != null) {
                            binding.layoutAvailableSlots.visibility = View.VISIBLE
                            binding.tvAvailableSlots.text = station.availableSlots.toString()
                        } else {
                            binding.layoutAvailableSlots.visibility = View.GONE
                        }
                        
                        // Display slots
                        if (station.slots.isNotEmpty()) {
                            binding.cardSlotsInfo.visibility = View.VISIBLE
                            slotsAdapter.submitList(station.slots)
                        } else {
                            binding.cardSlotsInfo.visibility = View.GONE
                        }
                        
                        binding.cardStationInfo.visibility = View.VISIBLE
                        
                    } else {
                        showError(getString(R.string.error_loading_station))
                    }
                } else {
                    val errorMsg = res.body()?.message ?: getString(R.string.error_loading_station)
                    showError(errorMsg)
                }
            } catch (t: Throwable) {
                showError(t.localizedMessage ?: getString(R.string.error_loading_station))
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        binding.cardStationInfo.visibility = View.GONE
        binding.cardSlotsInfo.visibility = View.GONE
    }
}
