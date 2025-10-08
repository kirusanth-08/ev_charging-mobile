package com.example.evcharger.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.card.MaterialCardView
import androidx.lifecycle.lifecycleScope
import com.example.evcharger.R
import com.example.evcharger.repository.StationRepository
import com.example.evcharger.auth.UserSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.evcharger.utils.StatusBarUtil

class OperatorDashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: UserSessionManager
    private val stationRepo = StationRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_dashboard)

        // Set green status bar to match hero card and toolbar
        StatusBarUtil.setGreen(this)

        sessionManager = UserSessionManager(this)

    val txtOperatorInfo = findViewById<TextView>(R.id.txtOperatorInfo)
    val cardScan = findViewById<MaterialCardView>(R.id.cardScan)
    val cardManage = findViewById<MaterialCardView>(R.id.cardManage)
        val txtStationSummary = findViewById<TextView>(R.id.txtStationSummary)
        val txtRecentScans = findViewById<TextView>(R.id.txtRecentScans)

        val username = sessionManager.loadSession().username
        if (username != null && username.isNotEmpty()) {
            txtOperatorInfo.text = "Welcome, $username"
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

        // Fetch station summary counts and populate station cards list
        val rvStations = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvOperatorStations)
        rvStations.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

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
                    }
                } else {
                    launch(Dispatchers.Main) {
                        txtStationSummary.text = "Stations: 0  •  Available slots: 0"
                    }
                }
            } catch (ex: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@OperatorDashboardActivity, "Failed to load stations", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
