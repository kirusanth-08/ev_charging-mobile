package com.example.evcharger.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.evcharger.databinding.ActivityDashboardBinding
import com.example.evcharger.viewmodel.DashboardViewModel
import com.example.evcharger.model.BackendSlot
import com.google.android.material.navigation.NavigationView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.example.evcharger.ui.fragments.MapsFragment
import com.example.evcharger.R

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

        // Top overlay button (z-layer above the map) — same action as bottom button
        binding.btnViewBookingsOverlay.setOnClickListener {
            startActivity(Intent(this, BookingListActivity::class.java).putExtra("NIC", nic))
        }

        vm.pendingCount.observe(this) { binding.txtPending.text = it.toString() }
        vm.approvedFutureCount.observe(this) { binding.txtApproved.text = it.toString() }

        vm.load(nic)
    }

    /**
     * Populate the drawer with slots for a selected station and open it.
     */
    fun showSlotsForStation(stationId: String, slots: List<BackendSlot>) {
        val navView = findViewById<NavigationView>(R.id.navView) ?: return
        val header = navView.getHeaderView(0) ?: return
        val rv = header.findViewById<RecyclerView>(R.id.rvSlots) ?: return

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = SlotAdapter(slots) { slot ->
            // When user selects a slot, start reservation form prefilled
            val intent = android.content.Intent(this, ReservationFormActivity::class.java)
            intent.putExtra("StationId", stationId)
            intent.putExtra("SlotNumber", slot.slotNumber)
            startActivity(intent)
            // close drawer
            findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawerLayout)?.closeDrawers()
        }

        findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawerLayout)?.open()
    }
}