package com.example.evcharger.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.evcharger.databinding.ActivityHomeBinding
import com.example.evcharger.repository.ReservationRepository
import com.example.evcharger.repository.ProfileRepository
import com.example.evcharger.utils.LocationUtils
import com.example.evcharger.auth.UserSessionManager
import com.example.evcharger.network.RetrofitClient
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
    private var isAccountActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val nic = intent.getStringExtra("NIC") ?: ""

        val repo = ReservationRepository()

        // Check account status first
        checkAccountStatus(nic)
        
        setupUI(nic)
        loadUpcomingBooking(nic, repo)
        loadStats(nic, repo)
    }
    
    private fun checkAccountStatus(nic: String) {
        lifecycleScope.launch {
            try {
                val sessionManager = UserSessionManager(this@HomeActivity)
                val session = sessionManager.loadSession()
                session.token?.let { token ->
                    RetrofitClient.setAuthToken(token)
                    
                    val profileRepo = ProfileRepository()
                    val response = withContext(Dispatchers.IO) { profileRepo.getProfile(nic) }
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val profile = response.body()?.data
                        isAccountActive = profile?.isActive ?: false
                        
                        if (!isAccountActive) {
                            showDeactivatedAccountUI()
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently continue if profile check fails
            }
        }
    }
    
    private fun showDeactivatedAccountUI() {
        // Hide booking-related features
        binding.btnBookNow.visibility = View.GONE
        binding.btnManageBooking.visibility = View.GONE
        binding.btnQuickHistory.visibility = View.GONE
        
        // Show deactivation notice
        binding.txtUpcomingBooking.text = "⚠️ Your account is deactivated\n\n" +
                "You can still view charging stations on the map, but booking features are disabled.\n\n" +
                "To reactivate your account, please contact:\n📧 contact@evcharge.com"
        binding.txtUpcomingBooking.visibility = View.VISIBLE
    }
    
    private fun setupUI(nic: String) {
        binding.btnBookNow.setOnClickListener {
            if (!isAccountActive) {
                showDeactivatedDialog()
                return@setOnClickListener
            }
            // Check if location is enabled before navigating to find stations
            LocationUtils.checkAndPromptLocationEnabled(this) {
                startActivity(Intent(this, DashboardActivity::class.java).putExtra("NIC", nic))
            }
        }

        binding.btnManageBooking.setOnClickListener {
            if (!isAccountActive) {
                showDeactivatedDialog()
                return@setOnClickListener
            }
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
            if (!isAccountActive) {
                showDeactivatedDialog()
                return@setOnClickListener
            }
            startActivity(Intent(this, BookingListActivity::class.java).putExtra("NIC", nic))
        }
        
        binding.btnQuickHelp.setOnClickListener {
            // TODO: Add help/support screen
            android.widget.Toast.makeText(this, "Help & Support - Coming Soon!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showDeactivatedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Account Deactivated")
            .setMessage(
                "Your account is currently deactivated.\n\n" +
                "Booking features are disabled. You can still view charging stations on the map.\n\n" +
                "To reactivate your account, please contact:\n📧 contact@evcharge.com"
            )
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun loadUpcomingBooking(nic: String, repo: ReservationRepository) {
        // Skip loading bookings for deactivated accounts
        if (!isAccountActive) {
            return
        }
        
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
                    val errorMsg = res.body()?.message ?: "Failed to load upcoming"
                    binding.txtUpcomingBooking.text = errorMsg
                }
            } catch (t: Throwable) {
                binding.txtUpcomingBooking.text = t.localizedMessage ?: "Error"
            }
        }
    }
    
    private fun loadStats(nic: String, repo: ReservationRepository) {
        // Skip loading stats for deactivated accounts
        if (!isAccountActive) {
            return
        }
        
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
