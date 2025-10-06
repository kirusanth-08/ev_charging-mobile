package com.example.evcharger.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.evcharger.databinding.ActivityHomeBinding
import com.example.evcharger.repository.ReservationRepository
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

        binding.btnBookNow.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java).putExtra("NIC", nic))
        }

        binding.btnManageBooking.setOnClickListener {
            startActivity(Intent(this, BookingListActivity::class.java).putExtra("NIC", nic))
        }

        binding.btnProfileTop.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Load upcoming booking inline
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { repo.getUpcoming(nic) }
                if (res.isSuccessful) {
                    val list = res.body()?.data
                    if (!list.isNullOrEmpty()) {
                        val next = list.first()
                        binding.txtUpcomingBooking.text = "Station: ${'$'}{next.stationName}\nStart: ${'$'}{next.startTime}"
                    } else binding.txtUpcomingBooking.text = "No upcoming bookings"
                } else {
                    binding.txtUpcomingBooking.text = "Failed to load upcoming"
                }
            } catch (t: Throwable) {
                binding.txtUpcomingBooking.text = t.localizedMessage ?: "Error"
            }
        }
    }
}
