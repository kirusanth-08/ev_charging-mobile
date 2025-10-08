package com.example.evcharger.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.evcharger.databinding.ActivityHomeBinding
import com.example.evcharger.repository.ReservationRepository
import com.example.evcharger.utils.LocationUtils
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * HomeActivity - a lighter-weight landing screen after login.
 * Shows quick actions: Book a charging point + Upcoming booking preview.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val nic = intent.getStringExtra("NIC") ?: ""

        val repo = ReservationRepository()

        setupUI(nic)
        loadUpcomingBooking(nic, repo)
        loadStats(nic, repo)
    }
    
    private fun setupUI(nic: String) {
        binding.btnBookNow.setOnClickListener {
            // Check if location is enabled before navigating to find stations
            LocationUtils.checkAndPromptLocationEnabled(this) {
                startActivity(Intent(this, DashboardActivity::class.java).putExtra("NIC", nic))
            }
        }

        binding.btnManageBooking.setOnClickListener {
            startActivity(Intent(this, BookingListActivity::class.java).putExtra("NIC", nic))
        }

        binding.btnProfileTop.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        
        // Quick action buttons
        binding.btnQuickProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        
        binding.btnQuickHistory.setOnClickListener {
            startActivity(Intent(this, BookingListActivity::class.java).putExtra("NIC", nic))
        }
        
        binding.btnQuickHelp.setOnClickListener {
            // TODO: Add help/support screen
            android.widget.Toast.makeText(this, "Help & Support - Coming Soon!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadUpcomingBooking(nic: String, repo: ReservationRepository) {
        // Load upcoming booking inline
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { repo.getUpcoming(nic) }
                if (res.isSuccessful) {
                    val list = res.body()?.data
                    if (!list.isNullOrEmpty()) {
                        val next = list.first()
                        binding.txtUpcomingBooking.text = "Station: ${next.stationName}\nStart: ${next.startTime}"
                    } else binding.txtUpcomingBooking.text = "No upcoming bookings"
                } else {
                    binding.txtUpcomingBooking.text = "Failed to load upcoming"
                }
            } catch (t: Throwable) {
                binding.txtUpcomingBooking.text = t.localizedMessage ?: "Error"
            }
        }
    }
    
    private fun loadStats(nic: String, repo: ReservationRepository) {
        lifecycleScope.launch {
            try {
                // Get upcoming bookings
                val upcomingRes = withContext(Dispatchers.IO) { repo.getUpcoming(nic) }
                
                // Get history bookings
                val historyRes = withContext(Dispatchers.IO) { repo.getHistory(nic) }
                
                // Calculate total bookings
                val upcomingCount = if (upcomingRes.isSuccessful) {
                    upcomingRes.body()?.data?.size ?: 0
                } else 0
                
                val historyCount = if (historyRes.isSuccessful) {
                    historyRes.body()?.data?.size ?: 0
                } else 0
                
                val totalCount = upcomingCount + historyCount
                binding.txtTotalBookings.text = totalCount.toString()
                binding.txtActiveCount.text = upcomingCount.toString()
                
            } catch (t: Throwable) {
                // Silently fail stats loading
                binding.txtTotalBookings.text = "-"
                binding.txtActiveCount.text = "-"
            }
        }
    }
}
