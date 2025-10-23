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
import com.example.evcharger.repository.ProfileRepository
import com.example.evcharger.auth.UserSessionManager
import com.example.evcharger.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.evcharger.utils.StatusBarUtil

/**
 * Shows upcoming and past bookings in a list (RecyclerView).
 */
class BookingListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingListBinding
    private lateinit var nic: String
    private val adapter = ReservationAdapter()
    private val repo = ReservationRepository()
    private var loadJob: Job? = null
    private var isAccountActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set green status bar to match header
        StatusBarUtil.setGreen(this)

        nic = intent.getStringExtra("NIC") ?: ""

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadData() }

        // Back button - navigate back to previous screen
        binding.btnBack?.setOnClickListener {
            finish()
        }

        checkAccountStatus()
        loadData()
    }
    
    private fun checkAccountStatus() {
        lifecycleScope.launch {
            try {
                val sessionManager = UserSessionManager(this@BookingListActivity)
                val session = sessionManager.loadSession()
                session.token?.let { token ->
                    RetrofitClient.setAuthToken(token)
                    
                    val profileRepo = ProfileRepository()
                    val response = withContext(Dispatchers.IO) { profileRepo.getProfile(nic) }
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val profile = response.body()?.data
                        isAccountActive = profile?.isActive ?: false
                    }
                }
            } catch (e: Exception) {
                // Continue if check fails
            }
        }
    }

    private fun loadData() {
        loadJob?.cancel()
        binding.swipeRefresh.isRefreshing = true

        loadJob = lifecycleScope.launch {
            try {
                // Show message for deactivated accounts
                if (!isAccountActive) {
                    adapter.submitList(emptyList())
                    Snackbar.make(
                        binding.root, 
                        "Your account is deactivated. No bookings available.", 
                        Snackbar.LENGTH_LONG
                    ).show()
                    binding.swipeRefresh.isRefreshing = false
                    return@launch
                }
                
                val upcoming = withContext(Dispatchers.IO) { repo.getUpcoming(nic) }
                val history = withContext(Dispatchers.IO) { repo.getHistory() }
                if (upcoming.isSuccessful && history.isSuccessful) {
                    val list = mutableListOf<Reservation>()
                    upcoming.body()?.data?.let { list.addAll(it) }
                    // Note: history returns BookingResponseData, not Reservation
                    // If you need to show history, convert BookingResponseData to Reservation or use separate adapter
                    adapter.submitList(list)
                } else {
                    val errorMsg = upcoming.body()?.message 
                        ?: history.body()?.message 
                        ?: "Failed to load bookings"
                    Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                Snackbar.make(binding.root, t.localizedMessage ?: "Error loading bookings", Snackbar.LENGTH_LONG).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}