package com.example.evcharger.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.evcharger.databinding.ActivityDashboardBinding
import com.example.evcharger.viewmodel.DashboardViewModel
import com.example.evcharger.ui.fragments.MapsFragment

/**
 * Dashboard shows counters and embedded map fragment.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val vm: DashboardViewModel by viewModels()
    private lateinit var nic: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nic = intent.getStringExtra("NIC") ?: ""

        supportFragmentManager.beginTransaction()
            .replace(binding.mapContainer.id, MapsFragment())
            .commit()

        binding.btnViewBookings.setOnClickListener {
            startActivity(Intent(this, BookingListActivity::class.java).putExtra("NIC", nic))
        }

        vm.pendingCount.observe(this) { binding.txtPending.text = it.toString() }
        vm.approvedFutureCount.observe(this) { binding.txtApproved.text = it.toString() }

        vm.load(nic)
    }
}