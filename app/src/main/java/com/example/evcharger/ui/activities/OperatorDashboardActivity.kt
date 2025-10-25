package com.example.evcharger.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.evcharger.R
import com.example.evcharger.repository.StationRepository
import com.example.evcharger.auth.UserSessionManager
import com.example.evcharger.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.evcharger.utils.StatusBarUtil

class OperatorDashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: UserSessionManager
    private val stationRepo = StationRepository()
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var txtStationSummary: TextView
    private lateinit var txtRecentScans: TextView
    private lateinit var rvStations: androidx.recyclerview.widget.RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_dashboard)

        // Set green status bar to match hero card and toolbar
        StatusBarUtil.setGreen(this)

        sessionManager = UserSessionManager(this)

        // Setup toolbar with menu
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.operatorTopAppBar)
        setSupportActionBar(toolbar)

        // Initialize views
        swipeRefresh = findViewById(R.id.swipeRefresh)
        txtStationSummary = findViewById(R.id.txtStationSummary)
        txtRecentScans = findViewById(R.id.txtRecentScans)
        rvStations = findViewById(R.id.rvOperatorStations)

        val txtOperatorInfo = findViewById<TextView>(R.id.txtOperatorInfo)
        val cardScan = findViewById<MaterialCardView>(R.id.cardScan)
        val cardManage = findViewById<MaterialCardView>(R.id.cardManage)
        val cardPendingBookings = findViewById<MaterialCardView>(R.id.cardPendingBookings)
        val cardConfirmedArrivals = findViewById<MaterialCardView>(R.id.cardConfirmedArrivals)

        val username = sessionManager.loadSession().username
        if (username != null && username.isNotEmpty()) {
            txtOperatorInfo.text = "Welcome, $username"
        }

        // Setup swipe to refresh
        swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.success,
            R.color.accent
        )
        swipeRefresh.setOnRefreshListener {
            loadStationData()
        }

        cardScan.setOnClickListener {
            val i = Intent(this, QRScannerActivity::class.java)
            i.putExtra("autoScan", true)
            startActivity(i)
        }

        cardManage.setOnClickListener {
            val i = Intent(this, ManageStationActivity::class.java)
            startActivity(i)
        }

        cardPendingBookings.setOnClickListener {
            val i = Intent(this, OperatorPendingBookingsActivity::class.java)
            startActivity(i)
        }

        cardConfirmedArrivals.setOnClickListener {
            val i = Intent(this, OperatorConfirmedArrivalsActivity::class.java)
            startActivity(i)
        }

        // Setup RecyclerView
        rvStations.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        // Load initial data
        loadStationData()
    }

    private fun loadStationData() {
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resp = stationRepo.getOperatorStations()
                if (resp.isSuccessful) {
                    val data = resp.body()?.data ?: emptyList()
                    val stationCount = data.size
                    var totalSlots = 0
                    var availableSlots = 0
                    for (s in data) {
                        val slots = s.slots ?: emptyList()
                        totalSlots += slots.size
                        for (slot in slots) {
                            if (slot.isAvailable == true) availableSlots++
                        }
                    }
                    launch(Dispatchers.Main) {
                        txtStationSummary.text = "Stations: $stationCount  •  Available slots: $availableSlots / $totalSlots"
                        txtRecentScans.text = "Recent scans: —"
                        rvStations.adapter = StationCardAdapter(data)
                        swipeRefresh.isRefreshing = false
                    }
                } else {
                    launch(Dispatchers.Main) {
                        txtStationSummary.text = "Stations: 0  •  Available slots: 0"
                        val errorMsg = resp.body()?.message ?: "Failed to load stations"
                        Toast.makeText(this@OperatorDashboardActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        swipeRefresh.isRefreshing = false
                    }
                }
            } catch (ex: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@OperatorDashboardActivity, ex.localizedMessage ?: "Failed to load stations", Toast.LENGTH_SHORT).show()
                    swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_operator_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch(Dispatchers.IO) {
            sessionManager.clearSession()
            RetrofitClient.setAuthToken(null)
            
            launch(Dispatchers.Main) {
                Toast.makeText(this@OperatorDashboardActivity, "Logged out successfully", Toast.LENGTH_SHORT).show()
                navigateToLogin()
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
