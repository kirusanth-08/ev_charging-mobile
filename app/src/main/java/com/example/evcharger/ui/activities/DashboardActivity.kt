package com.example.evcharger.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.evcharger.databinding.ActivityDashboardBinding
import com.example.evcharger.viewmodel.DashboardViewModel
import com.example.evcharger.model.BackendSlot
import com.example.evcharger.utils.LocationUtils
import com.example.evcharger.utils.NetworkMonitor
import com.example.evcharger.utils.ErrorBarManager
import com.google.android.material.navigation.NavigationView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.example.evcharger.ui.fragments.MapsFragment
import com.example.evcharger.R
import com.example.evcharger.utils.StatusBarUtil

/**
 * Dashboard shows counters and embedded map fragment.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val vm: DashboardViewModel by viewModels()
    private lateinit var nic: String
    
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var errorBarManager: ErrorBarManager
    private var wasDisconnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable edge-to-edge for full-screen map experience
        StatusBarUtil.enableEdgeToEdge(this)

        nic = intent.getStringExtra("NIC") ?: ""
        
        // Initialize network monitoring and error bar
        setupNetworkMonitoring()
        
        // Check location is enabled on activity start
        if (!LocationUtils.isLocationEnabled(this)) {
            LocationUtils.showEnableLocationDialog(
                this,
                onEnabled = {
                    // User will return after enabling location
                },
                onCancelled = {
                    // User cancelled, but let them stay on the screen
                    android.widget.Toast.makeText(
                        this,
                        "Enable location for better experience",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        supportFragmentManager.beginTransaction()
            .replace(binding.mapContainer.id, MapsFragment())
            .commit()

        setupUI()
    }
    
    override fun onResume() {
        super.onResume()
        // Start network monitoring
        networkMonitor.startMonitoring()
        
        // Recheck location when user returns (e.g., from settings)
        if (LocationUtils.isLocationEnabled(this)) {
            // Location is now enabled, refresh the map
            val frag = supportFragmentManager.findFragmentById(binding.mapContainer.id)
            if (frag is MapsFragment) {
                frag.refreshIfLocationEnabled()
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Stop network monitoring to save battery
        networkMonitor.stopMonitoring()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cleanup error bar resources
        errorBarManager.cleanup()
    }
    
    private fun setupNetworkMonitoring() {
        // Initialize network monitor
        networkMonitor = NetworkMonitor.getInstance(this)
        
        // Initialize error bar manager
        errorBarManager = ErrorBarManager(binding.errorBar.root)
        
        // Observe network status changes
        networkMonitor.isConnected.observe(this) { isConnected ->
            handleNetworkStatusChange(isConnected)
        }
    }
    
    private fun handleNetworkStatusChange(isConnected: Boolean) {
        when {
            isConnected && wasDisconnected -> {
                // Network reconnected after being disconnected
                errorBarManager.showNetworkConnected()
                wasDisconnected = false
            }
            !isConnected && !wasDisconnected -> {
                // Network just disconnected
                errorBarManager.showNetworkDisconnected()
                wasDisconnected = true
            }
            !isConnected && wasDisconnected -> {
                // Still disconnected, show reconnecting message
                errorBarManager.showNetworkReconnecting()
            }
        }
    }
    
    private fun setupUI() {
        // Back button - navigate to HomeActivity
        binding.btnBack.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Refresh button - reload stations
        binding.btnRefresh.setOnClickListener {
            refreshStations()
        }

        binding.btnViewBookings.setOnClickListener {
            startActivity(Intent(this, BookingListActivity::class.java).putExtra("NIC", nic))
        }

        // Current location button — find the fragment and call the helper
        binding.btnCurrentLocation.setOnClickListener {
            // Check if location is enabled before centering
            LocationUtils.checkAndPromptLocationEnabled(this) {
                val frag = supportFragmentManager.findFragmentById(binding.mapContainer.id)
                if (frag is MapsFragment) {
                    frag.centerOnCurrentLocation()
                }
            }
        }
        
        // Toggle view mode button - switch between nearby and all stations
        binding.btnToggleView.setOnClickListener {
            val frag = supportFragmentManager.findFragmentById(binding.mapContainer.id)
            if (frag is MapsFragment) {
                frag.toggleStationView()
            }
        }
    }
    
    /**
     * Refresh stations data from the server
     */
    private fun refreshStations() {
        val frag = supportFragmentManager.findFragmentById(binding.mapContainer.id)
        if (frag is MapsFragment) {
            // Show loading indicator
            showLoadingIndicator()
            
            // Trigger refresh in the MapsFragment
            frag.refreshStations()
            
            // Show feedback to user
            android.widget.Toast.makeText(
                this,
                "Refreshing stations...",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Update toggle button text based on current view mode
     * @param isShowingAllStations true if currently showing all stations
     */
    fun updateToggleButtonText(isShowingAllStations: Boolean) {
        runOnUiThread {
            if (isShowingAllStations) {
                binding.btnToggleView.text = "Nearby Only"
                binding.btnToggleView.icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation, theme)
            } else {
                binding.btnToggleView.text = "All Stations"
                binding.btnToggleView.icon = resources.getDrawable(android.R.drawable.ic_menu_mapmode, theme)
            }
        }
    }
    
    /**
     * Show loading indicator while fetching stations from API
     */
    fun showLoadingIndicator() {
        runOnUiThread {
            binding.cardLoadingIndicator.visibility = View.VISIBLE
        }
    }
    
    /**
     * Hide loading indicator after stations are fetched
     */
    fun hideLoadingIndicator() {
        runOnUiThread {
            binding.cardLoadingIndicator.visibility = View.GONE
        }
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