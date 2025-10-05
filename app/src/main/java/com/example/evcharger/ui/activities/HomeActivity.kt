package com.example.evcharger.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.evcharger.databinding.ActivityHomeBinding

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

        binding.btnBookNow.setOnClickListener {
            // For now, navigate to Dashboard which contains map and station list
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        binding.btnViewBookingsHome.setOnClickListener {
            startActivity(Intent(this, BookingListActivity::class.java))
        }

        binding.btnProfileTop.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // TODO: load upcoming booking using ReservationRepository or ViewModel
    }
}
