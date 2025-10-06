package com.example.evcharger.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.evcharger.databinding.ActivityOperatorDashboardBinding
import android.content.Intent
import com.example.evcharger.ui.activities.QRScannerActivity
import com.google.android.material.snackbar.Snackbar

class OperatorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperatorDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: implement operator-specific features (scan, confirm, station management)
        binding.txtOperatorInfo.text = "Welcome, Station Operator"

        // Launch scanner activity to handle operator login / scan / confirm flows
        binding.btnScan.setOnClickListener {
            val i = Intent(this, QRScannerActivity::class.java)
            i.putExtra("autoScan", true)
            startActivity(i)
        }

        binding.btnConfirm.setOnClickListener {
            // Delegate confirm flow to QRScannerActivity; open scanner flow for operator
            val i = Intent(this, QRScannerActivity::class.java)
            i.putExtra("autoScan", true)
            Snackbar.make(binding.root, "Open Scanner to confirm a booking", Snackbar.LENGTH_SHORT).show()
            startActivity(i)
        }

        binding.btnManageStation.setOnClickListener {
            startActivity(Intent(this, ManageStationActivity::class.java))
        }
    }
}
